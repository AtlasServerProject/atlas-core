package io.atlas.modules.economy;

import io.atlas.modules.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class EconomyRepository {

    public void updateBalance(UUID uuid, double balance) {
        String sql = """
                UPDATE economy_accounts
                SET balance = ?, updated_at = NOW()
                WHERE player_id = (
                    SELECT id FROM players WHERE uuid = ?
                )
                """;

        try (PreparedStatement statement = DatabaseManager.getConnection().prepareStatement(sql)) {
            statement.setDouble(1, balance);
            statement.setObject(2, uuid);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RuntimeException("Erro ao atualizar saldo do jogador.", exception);
        }
    }
}