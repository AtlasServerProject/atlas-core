package io.atlas.modules.player.repository;

import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.player.model.PlayerProfile;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class PlayerRepository {

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
