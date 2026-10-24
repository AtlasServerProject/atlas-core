package io.atlas.modules.rules.repository;

import io.atlas.modules.database.DatabaseConfig;
import java.sql.*;
import java.util.UUID;

public final class RulesRepository {
    private Connection connect() throws SQLException {
        var props = new java.util.Properties();
        props.setProperty("user", DatabaseConfig.USER);
        props.setProperty("password", DatabaseConfig.PASSWORD);
        props.setProperty("connectTimeout", "3");
        props.setProperty("socketTimeout", "5");
        return DriverManager.getConnection(DatabaseConfig.jdbcUrl(), props);
    }
    public boolean accepted(UUID player, int version) {
        try (var c = connect(); var p = c.prepareStatement(
                "SELECT 1 FROM player_rules_acceptance WHERE uuid=? AND rules_version=?")) {
            p.setObject(1, player); p.setInt(2, version);
            try (var r = p.executeQuery()) { return r.next(); }
        } catch (SQLException e) { throw new IllegalStateException("Falha ao consultar aceite das regras", e); }
    }
    public void accept(UUID player, int version) {
        try (var c = connect(); var p = c.prepareStatement(
                "INSERT INTO player_rules_acceptance(uuid,rules_version) VALUES(?,?) ON CONFLICT DO NOTHING")) {
            p.setObject(1, player); p.setInt(2, version); p.executeUpdate();
        } catch (SQLException e) { throw new IllegalStateException("Falha ao salvar aceite das regras", e); }
    }
}
