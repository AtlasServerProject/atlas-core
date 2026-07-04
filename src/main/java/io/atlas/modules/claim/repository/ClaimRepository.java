package io.atlas.modules.claim.repository;

import io.atlas.modules.claim.model.Claim;
import io.atlas.modules.claim.model.TrustLevel;
import io.atlas.modules.database.DatabaseManager;

import java.sql.*;
import java.util.*;

public final class ClaimRepository {

    public Optional<Claim> findAt(String world, int x, int z) {
        String sql = """
                SELECT c.*, p.uuid, p.username FROM claims c
                INNER JOIN players p ON p.id = c.owner_id
                WHERE c.world = ? AND ? BETWEEN c.min_x AND c.max_x
                  AND ? BETWEEN c.min_z AND c.max_z LIMIT 1
                """;
        return findOne(sql, world, x, z);
    }

    public boolean overlaps(String world, int minX, int minZ, int maxX, int maxZ) {
        String sql = """
                SELECT EXISTS(SELECT 1 FROM claims WHERE world = ?
                AND min_x <= ? AND max_x >= ? AND min_z <= ? AND max_z >= ?)
                """;
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, maxX);
            statement.setInt(3, minX);
            statement.setInt(4, maxZ);
            statement.setInt(5, minZ);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getBoolean(1);
            }
        } catch (SQLException exception) {
            throw failure("verificar sobreposição", exception);
        }
    }

    public int usedArea(UUID uuid) {
        String sql = """
                SELECT COALESCE(SUM((max_x-min_x+1)*(max_z-min_z+1)), 0)
                FROM claims c INNER JOIN players p ON p.id=c.owner_id WHERE p.uuid=?
                """;
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        } catch (SQLException exception) {
            throw failure("somar área", exception);
        }
    }

    public void create(UUID uuid, String world, int minX, int minZ, int maxX, int maxZ) {
        String sql = """
                INSERT INTO claims(owner_id,world,min_x,min_z,max_x,max_z)
                SELECT id,?,?,?,?,? FROM players WHERE uuid=?
                """;
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setString(1, world);
            statement.setInt(2, minX);
            statement.setInt(3, minZ);
            statement.setInt(4, maxX);
            statement.setInt(5, maxZ);
            statement.setObject(6, uuid);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw failure("criar claim", exception);
        }
    }

    public List<Claim> findOwned(UUID uuid) {
        String sql = """
                SELECT c.*,p.uuid,p.username FROM claims c
                INNER JOIN players p ON p.id=c.owner_id WHERE p.uuid=? ORDER BY c.id
                """;
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            try (ResultSet result = statement.executeQuery()) {
                List<Claim> claims = new ArrayList<>();
                while (result.next()) claims.add(map(result));
                return claims;
            }
        } catch (SQLException exception) {
            throw failure("listar claims", exception);
        }
    }

    public boolean deleteOwnedAt(UUID uuid, String world, int x, int z) {
        String sql = """
                DELETE FROM claims WHERE owner_id=(SELECT id FROM players WHERE uuid=?)
                AND world=? AND ? BETWEEN min_x AND max_x AND ? BETWEEN min_z AND max_z
                """;
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setObject(1, uuid);
            statement.setString(2, world);
            statement.setInt(3, x);
            statement.setInt(4, z);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw failure("abandonar claim", exception);
        }
    }

    public Optional<TrustLevel> trust(long claimId, UUID uuid) {
        String sql = """
                SELECT trust_level FROM claim_members m
                INNER JOIN players p ON p.id=m.player_id WHERE m.claim_id=? AND p.uuid=?
                """;
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setLong(1, claimId);
            statement.setObject(2, uuid);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        ? Optional.of(TrustLevel.valueOf(result.getString(1))) : Optional.empty();
            }
        } catch (SQLException exception) {
            throw failure("consultar confiança", exception);
        }
    }

    public boolean setTrust(long claimId, String username, TrustLevel level) {
        String sql = """
                INSERT INTO claim_members(claim_id,player_id,can_build,can_break,can_open,can_interact,trust_level)
                SELECT ?,id,?,?,?,?,? FROM players WHERE LOWER(username)=LOWER(?)
                ON CONFLICT(claim_id,player_id) DO UPDATE SET
                can_build=EXCLUDED.can_build,can_break=EXCLUDED.can_break,
                can_open=EXCLUDED.can_open,can_interact=EXCLUDED.can_interact,
                trust_level=EXCLUDED.trust_level
                """;
        boolean build = level == TrustLevel.BUILD;
        boolean container = level != TrustLevel.ACCESS;
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setLong(1, claimId);
            statement.setBoolean(2, build);
            statement.setBoolean(3, build);
            statement.setBoolean(4, container);
            statement.setBoolean(5, true);
            statement.setString(6, level.name());
            statement.setString(7, username);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw failure("conceder confiança", exception);
        }
    }

    public boolean removeTrust(long claimId, String username) {
        String sql = """
                DELETE FROM claim_members WHERE claim_id=? AND player_id=(
                SELECT id FROM players WHERE LOWER(username)=LOWER(?))
                """;
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            statement.setLong(1, claimId);
            statement.setString(2, username);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw failure("remover confiança", exception);
        }
    }

    private Optional<Claim> findOne(String sql, Object... values) {
        try (PreparedStatement statement = connection().prepareStatement(sql)) {
            for (int i=0;i<values.length;i++) statement.setObject(i+1, values[i]);
            try (ResultSet result=statement.executeQuery()) {
                return result.next()?Optional.of(map(result)):Optional.empty();
            }
        } catch (SQLException exception) { throw failure("carregar claim", exception); }
    }

    private Claim map(ResultSet r) throws SQLException {
        return new Claim(r.getLong("id"), r.getObject("uuid", UUID.class),
                r.getString("username"), r.getString("world"), r.getInt("min_x"),
                r.getInt("min_z"), r.getInt("max_x"), r.getInt("max_z"));
    }

    private Connection connection() { return DatabaseManager.getConnection(); }
    private RuntimeException failure(String action, SQLException e) {
        return new RuntimeException("Erro ao " + action + ".", e);
    }
}
