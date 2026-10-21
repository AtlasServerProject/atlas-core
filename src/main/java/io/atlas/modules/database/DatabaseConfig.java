package io.atlas.modules.database;

public class DatabaseConfig {
    private static final java.util.Properties CONFIG = load();

    public static final String HOST = CONFIG.getProperty("host", "localhost");
    public static final int PORT = Integer.parseInt(CONFIG.getProperty("port", "5432"));
    public static final String DATABASE = CONFIG.getProperty("database", "atlas");
    public static final String USER = CONFIG.getProperty("user", "atlas_app");
    public static final String PASSWORD = CONFIG.getProperty("password", "atlas");

    static {
        if (io.atlas.modules.lobby.service.DevEnvironment.DEVELOPMENT
                && (DATABASE.equals("atlas") || USER.equals("atlas_app") || PORT == 5432
                    || !CONFIG.containsKey("password"))) {
            throw new IllegalStateException("Lobby de testes exige banco, usuário, porta e credenciais próprios.");
        }
    }

    private static java.util.Properties load() {
        var properties = new java.util.Properties();
        var path = java.nio.file.Path.of("config", "atlas-database.properties");
        if (java.nio.file.Files.exists(path)) {
            try (var input = java.nio.file.Files.newInputStream(path)) { properties.load(input); }
            catch (java.io.IOException error) { throw new IllegalStateException("Configuração de banco inválida."); }
        }
        return properties;
    }

    public static String jdbcUrl() {
        return "jdbc:postgresql://" + HOST + ":" + PORT + "/" + DATABASE;
    }
}
