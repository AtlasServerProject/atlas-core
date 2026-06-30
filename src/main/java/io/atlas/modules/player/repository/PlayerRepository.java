package io.atlas.modules.player.repository;

import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.player.model.PlayerProfile;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class PlayerRepository {

    public void promotePremiumIdentity(UUID officialUuid, String username) {
        UUID offlineUuid = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)
        );
        if (officialUuid.equals(offlineUuid)) {
            return;
        }

        Connection connection = DatabaseManager.getConnection();
        boolean previousAutoCommit;
        try {
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao iniciar migração Premium.", exception);
        }

        try {
            Long officialPlayerId = findPlayerId(connection, officialUuid);
            Long offlinePlayerId = findPlayerId(connection, offlineUuid);

            if (offlinePlayerId == null) {
                connection.commit();
                return;
            }

            if (officialPlayerId == null) {
                updatePlayerIdentity(connection, offlinePlayerId, officialUuid, username);
            } else if (!officialPlayerId.equals(offlinePlayerId)) {
                mergePlayers(connection, officialPlayerId, offlinePlayerId);
                updatePlayerIdentity(connection, officialPlayerId, officialUuid, username);
            }

            connection.commit();
        } catch (SQLException exception) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                exception.addSuppressed(rollbackException);
            }
            throw new RuntimeException("Erro ao migrar identidade Offline para Premium.", exception);
        } finally {
            try {
                connection.setAutoCommit(previousAutoCommit);
            } catch (SQLException exception) {
                throw new RuntimeException("Erro ao restaurar conexão após migração Premium.", exception);
            }
        }
    }

    private Long findPlayerId(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id FROM players WHERE uuid = ? FOR UPDATE"
        )) {
            statement.setObject(1, uuid);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getLong("id") : null;
            }
        }
    }

    private void updatePlayerIdentity(
            Connection connection,
            long playerId,
            UUID officialUuid,
            String username
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE players
                SET uuid = ?, username = ?, updated_at = NOW()
                WHERE id = ?
                """)) {
            statement.setObject(1, officialUuid);
            statement.setString(2, username);
            statement.setLong(3, playerId);
            statement.executeUpdate();
        }
    }

    private void mergePlayers(
            Connection connection,
            long officialPlayerId,
            long offlinePlayerId
    ) throws SQLException {
        execute(connection, """
                UPDATE economy_accounts target
                SET balance = target.balance + source.balance,
                    total_earned = target.total_earned + source.total_earned,
                    total_spent = target.total_spent + source.total_spent,
                    updated_at = NOW()
                FROM economy_accounts source
                WHERE target.player_id = ? AND source.player_id = ?
                """, officialPlayerId, offlinePlayerId);
        execute(connection, "DELETE FROM economy_accounts WHERE player_id = ?", offlinePlayerId);

        execute(connection, """
                INSERT INTO player_ranks (player_id, rank_id, assigned_at, expires_at, active)
                SELECT ?, rank_id, assigned_at, expires_at, active
                FROM player_ranks WHERE player_id = ?
                ON CONFLICT (player_id, rank_id) DO UPDATE
                SET active = player_ranks.active OR EXCLUDED.active,
                    expires_at = COALESCE(
                        GREATEST(player_ranks.expires_at, EXCLUDED.expires_at),
                        player_ranks.expires_at,
                        EXCLUDED.expires_at
                    )
                """, officialPlayerId, offlinePlayerId);
        execute(connection, "DELETE FROM player_ranks WHERE player_id = ?", offlinePlayerId);

        execute(connection, """
                INSERT INTO player_keys (player_id, crate_id, quantity, updated_at)
                SELECT ?, crate_id, quantity, updated_at
                FROM player_keys WHERE player_id = ?
                ON CONFLICT (player_id, crate_id) DO UPDATE
                SET quantity = player_keys.quantity + EXCLUDED.quantity,
                    updated_at = NOW()
                """, officialPlayerId, offlinePlayerId);
        execute(connection, "DELETE FROM player_keys WHERE player_id = ?", offlinePlayerId);

        execute(connection, """
                INSERT INTO player_quests (
                    player_id, quest_id, progress, completed, completed_at, updated_at
                )
                SELECT ?, quest_id, progress, completed, completed_at, updated_at
                FROM player_quests WHERE player_id = ?
                ON CONFLICT (player_id, quest_id) DO UPDATE
                SET progress = GREATEST(player_quests.progress, EXCLUDED.progress),
                    completed = player_quests.completed OR EXCLUDED.completed,
                    completed_at = COALESCE(
                        player_quests.completed_at,
                        EXCLUDED.completed_at
                    ),
                    updated_at = NOW()
                """, officialPlayerId, offlinePlayerId);
        execute(connection, "DELETE FROM player_quests WHERE player_id = ?", offlinePlayerId);

        mergeAuthAccounts(connection, officialPlayerId, offlinePlayerId);

        for (String table : new String[]{
                "claim_members", "crate_open_history", "economy_transactions",
                "homes", "player_sessions", "player_vips",
                "pokemon_sell_history", "rtp_logs", "sell_history", "store_orders"
        }) {
            execute(
                    connection,
                    "UPDATE " + table + " SET player_id = ? WHERE player_id = ?",
                    officialPlayerId,
                    offlinePlayerId
            );
        }

        execute(connection, "DELETE FROM players WHERE id = ?", offlinePlayerId);
    }

    private void mergeAuthAccounts(
            Connection connection,
            long officialPlayerId,
            long offlinePlayerId
    ) throws SQLException {
        boolean officialAccountExists = accountExists(connection, officialPlayerId);
        if (!officialAccountExists) {
            execute(
                    connection,
                    "UPDATE auth_accounts SET player_id = ? WHERE player_id = ?",
                    officialPlayerId,
                    offlinePlayerId
            );
            return;
        }

        execute(connection, """
                UPDATE auth_accounts target
                SET password_hash = COALESCE(target.password_hash, source.password_hash),
                    last_ip = COALESCE(target.last_ip, source.last_ip),
                    is_premium = target.is_premium OR source.is_premium,
                    is_verified = target.is_verified OR source.is_verified,
                    last_login = COALESCE(
                        GREATEST(target.last_login, source.last_login),
                        target.last_login,
                        source.last_login
                    )
                FROM auth_accounts source
                WHERE target.player_id = ? AND source.player_id = ?
                """, officialPlayerId, offlinePlayerId);
        execute(connection, "DELETE FROM auth_accounts WHERE player_id = ?", offlinePlayerId);
    }

    private boolean accountExists(Connection connection, long playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM auth_accounts WHERE player_id = ?"
        )) {
            statement.setLong(1, playerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void execute(
            Connection connection,
            String sql,
            long firstPlayerId,
            long secondPlayerId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, firstPlayerId);
            statement.setLong(2, secondPlayerId);
            statement.executeUpdate();
        }
    }

    private void execute(Connection connection, String sql, long playerId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, playerId);
            statement.executeUpdate();
        }
    }

    public Optional<PlayerProfile> findByUuid(UUID uuid) {
        String sql = """
                SELECT
                    p.uuid,
                    p.username,
                    COALESCE(e.balance, 0) AS balance,
                    COALESCE(a.is_premium, false) AS premium,
                    p.first_login,
                    p.last_login
                FROM players p
                LEFT JOIN economy_accounts e ON e.player_id = p.id
                LEFT JOIN auth_accounts a ON a.player_id = p.id
                WHERE p.uuid = ?
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }

                return Optional.of(new PlayerProfile(
                        (UUID) result.getObject("uuid"),
                        result.getString("username"),
                        result.getDouble("balance"),
                        result.getBoolean("premium"),
                        toLocalDateTime(result.getTimestamp("first_login")),
                        toLocalDateTime(result.getTimestamp("last_login"))
                ));
            }

        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao buscar jogador por UUID.", exception);
        }
    }

    public void create(UUID uuid, String username) {
        String sql = """
                WITH inserted_player AS (
                    INSERT INTO players (uuid, username, first_login, last_login, is_online)
                    VALUES (?, ?, NOW(), NOW(), true)
                    RETURNING id
                )
                INSERT INTO economy_accounts (player_id, balance)
                SELECT id, 0 FROM inserted_player
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao criar jogador.", exception);
        }
    }

    public void updateLogin(UUID uuid, String username) {
        String sql = """
                UPDATE players
                SET username = ?, last_login = NOW(), is_online = true, updated_at = NOW()
                WHERE uuid = ?
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setObject(2, uuid);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao atualizar login do jogador.", exception);
        }
    }

    public void updateLogout(UUID uuid) {
        String sql = """
                UPDATE players
                SET is_online = false, updated_at = NOW()
                WHERE uuid = ?
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao atualizar logout do jogador.", exception);
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
