package io.atlas.modules.kit.repository;

import io.atlas.modules.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class DailyKitRepository {

    public Optional<Instant> findLastClaim(UUID playerUuid, String kitId) {
        String sql = """
                SELECT claimed_at
                FROM daily_kit_claims
                WHERE player_uuid = ? AND kit_id = ?
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, playerUuid);
            statement.setString(2, kitId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(result.getTimestamp("claimed_at").toInstant());
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao consultar resgate de kit diário.", exception);
        }
    }

    public void upsertClaim(UUID playerUuid, String kitId) {
        String sql = """
                INSERT INTO daily_kit_claims (player_uuid, kit_id, claimed_at)
                VALUES (?, ?, NOW())
                ON CONFLICT (player_uuid, kit_id)
                DO UPDATE SET claimed_at = EXCLUDED.claimed_at
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setObject(1, playerUuid);
            statement.setString(2, kitId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao registrar resgate de kit diário.", exception);
        }
    }
}
