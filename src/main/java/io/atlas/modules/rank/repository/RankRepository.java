package io.atlas.modules.rank.repository;

import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.rank.model.Rank;

import java.sql.*;
import java.util.*;

public class RankRepository {

    public Optional<UUID> findPlayerUuidByUsername(String username) {
        String sql = "SELECT uuid FROM players WHERE LOWER(username) = LOWER(?)";

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, username);

            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of((UUID) result.getObject("uuid"))
                        : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao buscar jogador pelo nome.", exception);
        }
    }

    public void assignRank(UUID playerUuid, long rankId) {
        String sql = """
                INSERT INTO player_ranks (player_id, rank_id, assigned_at, expires_at, active)
                SELECT p.id, ?, NOW(), NULL, true
                FROM players p
                WHERE p.uuid = ?
                ON CONFLICT (player_id, rank_id)
                DO UPDATE SET assigned_at = NOW(), expires_at = NULL, active = true
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setLong(1, rankId);
            statement.setObject(2, playerUuid);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao atribuir rank ao jogador.", exception);
        }
    }

    public void removeRank(UUID playerUuid, long rankId) {
        String sql = """
                UPDATE player_ranks pr
                SET active = false
                FROM players p
                WHERE pr.player_id = p.id
                AND p.uuid = ?
                AND pr.rank_id = ?
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, playerUuid);
            statement.setLong(2, rankId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao remover rank do jogador.", exception);
        }
    }

    public void addPermission(long rankId, String permission) {
        String sql = """
                INSERT INTO rank_permissions (rank_id, permission, enabled)
                VALUES (?, ?, true)
                ON CONFLICT (rank_id, permission)
                DO UPDATE SET enabled = true
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setLong(1, rankId);
            statement.setString(2, permission);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao adicionar permissão ao rank.", exception);
        }
    }

    public void removePermission(long rankId, String permission) {
        String sql = """
                UPDATE rank_permissions
                SET enabled = false
                WHERE rank_id = ?
                AND permission = ?
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setLong(1, rankId);
            statement.setString(2, permission);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao remover permissão do rank.", exception);
        }
    }

    public List<Rank> findAllRanks() {
        String sql = """
                SELECT id, identifier, display_name, prefix, color, priority, is_staff
                FROM ranks
                WHERE enabled = true
                ORDER BY priority DESC
                """;

        List<Rank> ranks = new ArrayList<>();

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                ranks.add(new Rank(
                        result.getLong("id"),
                        result.getString("identifier"),
                        result.getString("display_name"),
                        result.getString("prefix"),
                        result.getString("color"),
                        result.getInt("priority"),
                        result.getBoolean("is_staff")
                ));
            }

            return ranks;
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao carregar ranks.", exception);
        }
    }

    public Set<String> findPermissionsByRankId(long rankId) {
        String sql = """
                SELECT permission
                FROM rank_permissions
                WHERE rank_id = ?
                AND enabled = true
                """;

        Set<String> permissions = new HashSet<>();

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setLong(1, rankId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    permissions.add(result.getString("permission"));
                }
            }

            return permissions;
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao carregar permissões do rank.", exception);
        }
    }

    public List<Rank> findRanksByPlayerUuid(UUID uuid) {
        String sql = """
                SELECT r.id, r.identifier, r.display_name, r.prefix, r.color, r.priority, r.is_staff
                FROM player_ranks pr
                INNER JOIN ranks r ON r.id = pr.rank_id
                INNER JOIN players p ON p.id = pr.player_id
                WHERE p.uuid = ?
                AND pr.active = true
                AND r.enabled = true
                AND (pr.expires_at IS NULL OR pr.expires_at > NOW())
                ORDER BY r.priority DESC
                """;

        List<Rank> ranks = new ArrayList<>();

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ranks.add(new Rank(
                            result.getLong("id"),
                            result.getString("identifier"),
                            result.getString("display_name"),
                            result.getString("prefix"),
                            result.getString("color"),
                            result.getInt("priority"),
                            result.getBoolean("is_staff")
                    ));
                }
            }

            return ranks;
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao carregar ranks do jogador.", exception);
        }
    }
}
