package io.atlas.modules.lobby.repository;

import io.atlas.modules.database.DatabaseManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class LobbyNpcDestinationRepository {
    public record Destination(String label, String world, double x, double y, double z, float yaw, float pitch, String action) {}

    public Map<String, Destination> load() {
        try (var statement = DatabaseManager.getConnection().prepareStatement(
                "SELECT npc_tag,label,world,x,y,z,yaw,pitch,action FROM lobby_npc_destinations");
             var rows = statement.executeQuery()) {
            var result = new HashMap<String, Destination>();
            while (rows.next()) {
                result.put(rows.getString(1), new Destination(rows.getString(2), rows.getString(3),
                        rows.getDouble(4), rows.getDouble(5), rows.getDouble(6), rows.getFloat(7), rows.getFloat(8), rows.getString(9)));
            }
            return Map.copyOf(result);
        } catch (SQLException error) {
            throw new IllegalStateException("Destinos do lobby indisponíveis", error);
        }
    }
}
