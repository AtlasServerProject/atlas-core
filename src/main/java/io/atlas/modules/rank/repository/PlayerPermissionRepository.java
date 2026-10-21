package io.atlas.modules.rank.repository;

import io.atlas.modules.database.DatabaseManager;
import java.sql.SQLException;
import java.util.UUID;

public final class PlayerPermissionRepository {
    public boolean hasPermission(UUID uuid, String permission) {
        try (var statement = DatabaseManager.getConnection().prepareStatement("""
                SELECT EXISTS (
                    SELECT 1 FROM player_permissions permissions
                    JOIN players ON players.id = permissions.player_id
                    WHERE players.uuid = ? AND permissions.permission = ? AND permissions.enabled
                )
                """)) {
            statement.setObject(1, uuid);
            statement.setString(2, permission);
            try (var rows = statement.executeQuery()) { rows.next(); return rows.getBoolean(1); }
        } catch (SQLException error) {
            throw new IllegalStateException("Não foi possível consultar permissão individual.", error);
        }
    }
}
