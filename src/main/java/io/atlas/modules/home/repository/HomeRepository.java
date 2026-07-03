package io.atlas.modules.home.repository;

import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.home.model.Home;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class HomeRepository {

    public List<Home> findAll(UUID uuid) {
        String sql = """
                SELECT h.* FROM homes h
                INNER JOIN players p ON p.id = h.player_id
                WHERE p.uuid = ?
                ORDER BY h.is_primary DESC, LOWER(h.name)
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            try (ResultSet result = statement.executeQuery()) {
                List<Home> homes = new ArrayList<>();
                while (result.next()) {
                    homes.add(map(result));
                }
                return homes;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao listar homes.", exception);
        }
    }

    public Optional<Home> find(UUID uuid, String name) {
        String sql = """
                SELECT h.* FROM homes h
                INNER JOIN players p ON p.id = h.player_id
                WHERE p.uuid = ? AND LOWER(h.name) = LOWER(?)
                """;
        return findOne(sql, uuid, name);
    }

    public Optional<Home> findPrimary(UUID uuid) {
        String sql = """
                SELECT h.* FROM homes h
                INNER JOIN players p ON p.id = h.player_id
                WHERE p.uuid = ?
                ORDER BY h.is_primary DESC, h.created_at ASC
                LIMIT 1
                """;
        return findOne(sql, uuid);
    }

    public int count(UUID uuid) {
        String sql = """
                SELECT COUNT(*) FROM homes h
                INNER JOIN players p ON p.id = h.player_id
                WHERE p.uuid = ?
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao contar homes.", exception);
        }
    }

    public void save(UUID uuid, Home home, boolean firstHome) {
        String sql = """
                INSERT INTO homes (
                    player_id, name, world, x, y, z, yaw, pitch,
                    is_primary, created_at, updated_at
                )
                SELECT id, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()
                FROM players WHERE uuid = ?
                ON CONFLICT (player_id, (LOWER(name))) DO UPDATE
                SET world = EXCLUDED.world,
                    x = EXCLUDED.x,
                    y = EXCLUDED.y,
                    z = EXCLUDED.z,
                    yaw = EXCLUDED.yaw,
                    pitch = EXCLUDED.pitch,
                    updated_at = NOW()
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setString(1, home.name());
            statement.setString(2, home.world());
            statement.setDouble(3, home.x());
            statement.setDouble(4, home.y());
            statement.setDouble(5, home.z());
            statement.setFloat(6, home.yaw());
            statement.setFloat(7, home.pitch());
            statement.setBoolean(8, firstHome);
            statement.setObject(9, uuid);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao salvar home.", exception);
        }
    }

    public boolean delete(UUID uuid, String name) {
        String sql = """
                DELETE FROM homes
                WHERE player_id = (SELECT id FROM players WHERE uuid = ?)
                  AND LOWER(name) = LOWER(?)
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            statement.setString(2, name);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao excluir home.", exception);
        }
    }

    private Optional<Home> findOne(String sql, Object... values) {
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) {
                statement.setObject(index + 1, values[index]);
            }
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao carregar home.", exception);
        }
    }

    private Home map(ResultSet result) throws SQLException {
        return new Home(
                result.getLong("id"),
                result.getString("name"),
                result.getString("world"),
                result.getDouble("x"),
                result.getDouble("y"),
                result.getDouble("z"),
                result.getFloat("yaw"),
                result.getFloat("pitch"),
                result.getBoolean("is_primary")
        );
    }
}
