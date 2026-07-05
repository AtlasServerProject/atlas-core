package io.atlas.modules.performance.repository;

import io.atlas.modules.database.DatabaseManager;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ItemRecoveryRepository {
    public void store(UUID uuid, String source, List<String> items, Instant expiresAt) {
        String sql = "INSERT INTO item_recovery_entries(player_uuid,source,item_snbt,expires_at) VALUES (?,?,?,?)";
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            for (String item : items) {
                statement.setObject(1, uuid);
                statement.setString(2, source);
                statement.setString(3, item);
                statement.setTimestamp(4, Timestamp.from(expiresAt));
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao armazenar itens recuperáveis.", exception);
        }
    }

    public List<RecoveryEntry> findAvailable(UUID uuid, int limit) {
        String sql = """
                SELECT id,source,item_snbt,expires_at FROM item_recovery_entries
                WHERE player_uuid=? AND recovered_at IS NULL AND expires_at>NOW()
                ORDER BY created_at DESC,id DESC LIMIT ?
                """;
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            statement.setInt(2, limit);
            try (ResultSet result = statement.executeQuery()) {
                List<RecoveryEntry> entries = new ArrayList<>();
                while (result.next()) entries.add(new RecoveryEntry(result.getLong("id"),
                        result.getString("source"), result.getString("item_snbt"),
                        result.getTimestamp("expires_at").toInstant()));
                return entries;
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao carregar itens recuperáveis.", exception);
        }
    }

    public void markRecovered(List<Long> ids) {
        if (ids.isEmpty()) return;
        String sql = "UPDATE item_recovery_entries SET recovered_at=NOW() WHERE id=? AND recovered_at IS NULL";
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            for (Long id : ids) { statement.setLong(1, id); statement.addBatch(); }
            statement.executeBatch();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao confirmar recuperação de itens.", exception);
        }
    }

    public int deleteExpired() {
        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(
                "DELETE FROM item_recovery_entries WHERE expires_at<=NOW()")) {
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao expirar itens recuperáveis.", exception);
        }
    }

    public record RecoveryEntry(long id, String source, String itemSnbt, Instant expiresAt) {}
}
