package io.atlas.modules.rules;

import io.atlas.modules.database.DatabaseConfig;
import io.atlas.modules.rules.repository.RulesRepository;
import java.sql.*;
import java.util.UUID;

public final class RulesRepositoryTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        if (!DatabaseConfig.DATABASE.equals("atlas_compat_182") || DatabaseConfig.PORT != 25432)
            throw new IllegalStateException("Isolated compatibility database required");
        var id = UUID.randomUUID(); var other = UUID.randomUUID(); var repo = new RulesRepository();
        try (var c = DriverManager.getConnection(DatabaseConfig.jdbcUrl(), DatabaseConfig.USER, DatabaseConfig.PASSWORD)) {
            try {
                try (var p = c.prepareStatement("INSERT INTO players(uuid,username) VALUES(?,?)")) {
                    p.setObject(1,id); p.setString(2,"RulesFixture"); p.executeUpdate();
                }
                check(!repo.accepted(id,1), "new player has not accepted");
                repo.accept(id,1); repo.accept(id,1);
                check(new RulesRepository().accepted(id,1), "acceptance survives new connection and repeated acceptance");
                check(!repo.accepted(other,1), "acceptance isolated by player");
                check(!repo.accepted(id,2), "new version requires acceptance");
                repo.accept(id,2);
                check(repo.accepted(id,1) && repo.accepted(id,2), "version history preserved");
                try (var p=c.prepareStatement("SELECT count(*) FROM player_rules_acceptance WHERE uuid=?")) {
                    p.setObject(1,id); try(var r=p.executeQuery()){r.next();check(r.getInt(1)==2,"duplicates prevented");}
                }
            } finally {
                try(var p=c.prepareStatement("DELETE FROM player_rules_acceptance WHERE uuid=?")){p.setObject(1,id);p.executeUpdate();}
                try(var p=c.prepareStatement("DELETE FROM players WHERE uuid=?")){p.setObject(1,id);p.executeUpdate();}
            }
        }
        System.out.println("RulesRepositoryTest PASS: first access, durability, idempotency, per-player/version isolation");
    }
}
