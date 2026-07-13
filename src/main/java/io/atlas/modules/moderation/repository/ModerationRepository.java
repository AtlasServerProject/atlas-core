package io.atlas.modules.moderation.repository;

import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.moderation.model.Punishment;
import io.atlas.modules.moderation.model.PunishmentType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ModerationRepository {

    public Optional<PlayerRef> findPlayer(String username) {
        String sql = "SELECT id, uuid, username FROM players WHERE LOWER(username) = LOWER(?)";
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapPlayer(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao buscar jogador para moderação.", exception);
        }
    }

    public Optional<PlayerRef> findPlayer(UUID uuid) {
        String sql = "SELECT id, uuid, username FROM players WHERE uuid = ?";
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapPlayer(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao buscar jogador para moderação.", exception);
        }
    }

    public long create(
            PunishmentType type,
            PlayerRef target,
            String targetIp,
            PlayerRef actor,
            String actorName,
            String reason,
            Instant expiresAt
    ) {
        String sql = """
                INSERT INTO moderation_punishments (
                    type, target_player_id, target_name, target_ip,
                    actor_player_id, actor_name, reason, expires_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, type.name());
            if (target == null) statement.setObject(2, null);
            else statement.setLong(2, target.id());
            statement.setString(3, target == null ? targetIp : target.username());
            statement.setString(4, targetIp);
            if (actor == null) statement.setObject(5, null);
            else statement.setLong(5, actor.id());
            statement.setString(6, actorName);
            statement.setString(7, reason);
            statement.setTimestamp(8, expiresAt == null ? null : Timestamp.from(expiresAt));
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong("id");
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao registrar punição.", exception);
        }
    }

    public Optional<Punishment> findActivePlayerPunishment(UUID uuid, PunishmentType type) {
        String sql = """
                SELECT mp.* FROM moderation_punishments mp
                INNER JOIN players p ON p.id = mp.target_player_id
                WHERE p.uuid = ?
                  AND mp.type = ?
                  AND mp.revoked_at IS NULL
                  AND (mp.expires_at IS NULL OR mp.expires_at > NOW())
                ORDER BY mp.created_at DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            statement.setString(2, type.name());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapPunishment(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao buscar punição ativa.", exception);
        }
    }

    public Optional<Punishment> findActiveIpBan(String ip) {
        String sql = """
                SELECT * FROM moderation_punishments
                WHERE target_ip = ?
                  AND type = ?
                  AND revoked_at IS NULL
                  AND (expires_at IS NULL OR expires_at > NOW())
                ORDER BY created_at DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, ip);
            statement.setString(2, PunishmentType.BAN_IP.name());
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapPunishment(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao buscar banimento por IP.", exception);
        }
    }

    public int revokePlayerPunishments(UUID uuid, PunishmentType type, PlayerRef actor, String actorName, String reason) {
        String sql = """
                UPDATE moderation_punishments mp
                SET revoked_at = NOW(),
                    revoked_by_player_id = ?,
                    revoked_by_name = ?,
                    revoked_reason = ?
                FROM players p
                WHERE p.id = mp.target_player_id
                  AND p.uuid = ?
                  AND mp.type = ?
                  AND mp.revoked_at IS NULL
                  AND (mp.expires_at IS NULL OR mp.expires_at > NOW())
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            if (actor == null) statement.setObject(1, null);
            else statement.setLong(1, actor.id());
            statement.setString(2, actorName);
            statement.setString(3, reason);
            statement.setObject(4, uuid);
            statement.setString(5, type.name());
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao revogar punição.", exception);
        }
    }

    public int revokeIpBans(String ip, PlayerRef actor, String actorName, String reason) {
        String sql = """
                UPDATE moderation_punishments
                SET revoked_at = NOW(),
                    revoked_by_player_id = ?,
                    revoked_by_name = ?,
                    revoked_reason = ?
                WHERE target_ip = ?
                  AND type = ?
                  AND revoked_at IS NULL
                  AND (expires_at IS NULL OR expires_at > NOW())
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            if (actor == null) statement.setObject(1, null);
            else statement.setLong(1, actor.id());
            statement.setString(2, actorName);
            statement.setString(3, reason);
            statement.setString(4, ip);
            statement.setString(5, PunishmentType.BAN_IP.name());
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao revogar banimento por IP.", exception);
        }
    }

    public List<Punishment> history(String username, int limit) {
        String sql = """
                SELECT * FROM moderation_punishments
                WHERE LOWER(target_name) = LOWER(?)
                ORDER BY created_at DESC
                LIMIT ?
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setInt(2, limit);
            try (ResultSet result = statement.executeQuery()) {
                List<Punishment> punishments = new ArrayList<>();
                while (result.next()) {
                    punishments.add(mapPunishment(result));
                }
                return punishments;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao buscar histórico de punições.", exception);
        }
    }

    private PlayerRef mapPlayer(ResultSet result) throws SQLException {
        return new PlayerRef(
                result.getLong("id"),
                (UUID) result.getObject("uuid"),
                result.getString("username")
        );
    }

    private Punishment mapPunishment(ResultSet result) throws SQLException {
        return new Punishment(
                result.getLong("id"),
                PunishmentType.valueOf(result.getString("type")),
                result.getString("target_name"),
                result.getString("target_ip"),
                result.getString("actor_name"),
                result.getString("reason"),
                toInstant(result.getTimestamp("expires_at")),
                toInstant(result.getTimestamp("revoked_at")),
                result.getString("revoked_by_name"),
                result.getString("revoked_reason"),
                toInstant(result.getTimestamp("created_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    public record PlayerRef(long id, UUID uuid, String username) {
    }
}
