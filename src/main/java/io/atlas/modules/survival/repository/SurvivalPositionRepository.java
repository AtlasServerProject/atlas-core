package io.atlas.modules.survival.repository;

import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.survival.model.SurvivalPosition;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class SurvivalPositionRepository {

    public void save(UUID uuid, SurvivalPosition position) {
        String sql = """
                INSERT INTO survival_positions (player_id, x, y, z, yaw, pitch, updated_at)
                SELECT id, ?, ?, ?, ?, ?, NOW()
                FROM players
                WHERE uuid = ?
                ON CONFLICT (player_id) DO UPDATE
                SET x = EXCLUDED.x,
                    y = EXCLUDED.y,
                    z = EXCLUDED.z,
                    yaw = EXCLUDED.yaw,
                    pitch = EXCLUDED.pitch,
                    updated_at = NOW()
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setDouble(1, position.x());
            statement.setDouble(2, position.y());
            statement.setDouble(3, position.z());
            statement.setFloat(4, position.yaw());
            statement.setFloat(5, position.pitch());
            statement.setObject(6, uuid);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao salvar posição do Survival Emerald.", exception);
        }
    }

    public Optional<SurvivalPosition> find(UUID uuid) {
        String sql = """
                SELECT s.x, s.y, s.z, s.yaw, s.pitch
                FROM survival_positions s
                INNER JOIN players p ON p.id = s.player_id
                WHERE p.uuid = ?
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new SurvivalPosition(
                        result.getDouble("x"),
                        result.getDouble("y"),
                        result.getDouble("z"),
                        result.getFloat("yaw"),
                        result.getFloat("pitch")
                ));
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao carregar posição do Survival Emerald.", exception);
        }
    }

    public void delete(UUID uuid) {
        String sql = """
                DELETE FROM survival_positions
                WHERE player_id = (SELECT id FROM players WHERE uuid = ?)
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao remover posição do Survival Emerald.", exception);
        }
    }
}
