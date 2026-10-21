package io.atlas.modules.player.repository;
import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.player.model.ReconnectPosition;
import java.util.Optional;
import java.util.UUID;
public final class ReconnectPositionRepository {
    public Optional<ReconnectPosition> find(UUID uuid) {
        try(var p=DatabaseManager.getConnection().prepareStatement("SELECT dimension,x,y,z,yaw,pitch FROM player_reconnect_positions WHERE uuid=?")) {
            p.setObject(1,uuid);
            try(var r=p.executeQuery()) { return r.next()?Optional.of(new ReconnectPosition(r.getString(1),r.getDouble(2),r.getDouble(3),r.getDouble(4),r.getFloat(5),r.getFloat(6))):Optional.empty(); }
        } catch(java.sql.SQLException e) { throw new IllegalStateException("Erro ao carregar última localização",e); }
    }
    public void save(UUID uuid,ReconnectPosition pos) {
        try(var p=DatabaseManager.getConnection().prepareStatement("INSERT INTO player_reconnect_positions(uuid,dimension,x,y,z,yaw,pitch) VALUES(?,?,?,?,?,?,?) ON CONFLICT(uuid) DO UPDATE SET dimension=EXCLUDED.dimension,x=EXCLUDED.x,y=EXCLUDED.y,z=EXCLUDED.z,yaw=EXCLUDED.yaw,pitch=EXCLUDED.pitch,updated_at=NOW()")) {
            p.setObject(1,uuid);p.setString(2,pos.dimension());p.setDouble(3,pos.x());p.setDouble(4,pos.y());p.setDouble(5,pos.z());p.setFloat(6,pos.yaw());p.setFloat(7,pos.pitch());p.executeUpdate();
        } catch(java.sql.SQLException e) { throw new IllegalStateException("Erro ao salvar última localização",e); }
    }
}
