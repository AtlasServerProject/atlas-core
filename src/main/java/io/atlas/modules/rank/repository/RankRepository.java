package io.atlas.modules.rank.repository;

import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.rank.model.Rank;

import java.sql.*;
import java.util.*;

public class RankRepository {

    public List<Rank> findAllRanks() {
        String sql = """
                SELECT id, identifier, display_name, prefix, priority, is_staff
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
                SELECT r.id, r.identifier, r.display_name, r.prefix, r.priority, r.is_staff
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