package io.atlas.modules.moderation.repository;

import io.atlas.modules.moderation.model.StaffModeSession;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class StaffModeRepository {
    private final Supplier<Connection> connection;
    public StaffModeRepository(Supplier<Connection> connection) { this.connection = connection; }

    public boolean begin(StaffModeSession s) {
        String sql = """
                INSERT INTO staff_mode_sessions (player_uuid, game_mode, world, x, y, z, yaw, pitch,
                    may_fly, flying, invulnerable, instant_build, may_build, fly_speed, walk_speed)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (player_uuid) DO UPDATE SET
                    game_mode=EXCLUDED.game_mode, world=EXCLUDED.world, x=EXCLUDED.x, y=EXCLUDED.y, z=EXCLUDED.z,
                    yaw=EXCLUDED.yaw, pitch=EXCLUDED.pitch, may_fly=EXCLUDED.may_fly, flying=EXCLUDED.flying,
                    invulnerable=EXCLUDED.invulnerable, instant_build=EXCLUDED.instant_build,
                    may_build=EXCLUDED.may_build, fly_speed=EXCLUDED.fly_speed, walk_speed=EXCLUDED.walk_speed,
                    active=TRUE, started_at=NOW(), restored_at=NULL
                WHERE NOT staff_mode_sessions.active
                """;
        try (var statement = connection.get().prepareStatement(sql)) {
            Object[] values = {s.uuid(), s.gameMode(), s.world(), s.x(), s.y(), s.z(), s.yaw(), s.pitch(),
                    s.mayFly(), s.flying(), s.invulnerable(), s.instantBuild(), s.mayBuild(), s.flySpeed(), s.walkSpeed()};
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) { throw new IllegalStateException("Não foi possível salvar a sessão de StaffMode.", e); }
    }

    public Optional<StaffModeSession> findActive(UUID uuid) {
        try (var statement = connection.get().prepareStatement("SELECT * FROM staff_mode_sessions WHERE player_uuid=? AND active")) {
            statement.setObject(1, uuid);
            try (var r = statement.executeQuery()) {
                if (!r.next()) return Optional.empty();
                return Optional.of(new StaffModeSession(uuid, r.getString("game_mode"), r.getString("world"),
                        r.getDouble("x"), r.getDouble("y"), r.getDouble("z"), r.getFloat("yaw"), r.getFloat("pitch"),
                        r.getBoolean("may_fly"), r.getBoolean("flying"), r.getBoolean("invulnerable"),
                        r.getBoolean("instant_build"), r.getBoolean("may_build"), r.getFloat("fly_speed"), r.getFloat("walk_speed")));
            }
        } catch (SQLException e) { throw new IllegalStateException("Não foi possível recuperar a sessão de StaffMode.", e); }
    }

    public void complete(UUID uuid) {
        try (var statement = connection.get().prepareStatement(
                "UPDATE staff_mode_sessions SET active=FALSE, restored_at=NOW() WHERE player_uuid=? AND active")) {
            statement.setObject(1, uuid);
            statement.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Não foi possível encerrar a sessão de StaffMode.", e); }
    }
}
