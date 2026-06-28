package io.atlas.modules.database;

import io.atlas.AtlasMod;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static Connection connection;

    public static void connect() {
        try {
            connection = DriverManager.getConnection(
                    DatabaseConfig.jdbcUrl(),
                    DatabaseConfig.USER,
                    DatabaseConfig.PASSWORD
            );

            AtlasMod.LOGGER.info("PostgreSQL conectado com sucesso.");
        } catch (SQLException exception) {
            AtlasMod.LOGGER.error("Erro ao conectar no PostgreSQL.", exception);
        }
    }

    public static Connection getConnection() {
        return connection;
    }

    public static void disconnect() {
        if (connection == null) return;

        try {
            connection.close();
            AtlasMod.LOGGER.info("Conexão PostgreSQL encerrada.");
        } catch (SQLException exception) {
            AtlasMod.LOGGER.error("Erro ao fechar conexão PostgreSQL.", exception);
        }
    }
}