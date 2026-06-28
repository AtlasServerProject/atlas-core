package io.atlas.modules.database;

public class DatabaseConfig {

    public static final String HOST = "localhost";
    public static final int PORT = 5432;
    public static final String DATABASE = "atlas";
    public static final String USER = "atlas_app";
    public static final String PASSWORD = "atlas";

    public static String jdbcUrl() {
        return "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE;
    }
}