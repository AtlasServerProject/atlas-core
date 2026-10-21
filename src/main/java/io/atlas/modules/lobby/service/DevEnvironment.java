package io.atlas.modules.lobby.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public final class DevEnvironment {
    private static final Properties CONFIG = load();
    public static final boolean DEVELOPMENT = "development".equals(CONFIG.getProperty("mode"));
    public static final String KEY = CONFIG.getProperty("broker-key", "");
    public static final int BROKER_PORT = port("broker-port", 25567);
    public static final int DEV_PORT = port("dev-port", 25566);
    public static final int MAIN_PORT = port("main-port", 25565);
    public static final String HOST = CONFIG.getProperty("public-host", "").trim();
    public static final String DEV_HOST = CONFIG.getProperty("dev-public-host", HOST).trim();
    public static final String MAIN_HOST = CONFIG.getProperty("main-public-host", HOST).trim();
    public static final int DEV_PUBLIC_PORT = port("dev-public-port", DEV_PORT);
    public static final int MAIN_PUBLIC_PORT = port("main-public-port", MAIN_PORT);
    public static final String USER_RUNTIME_DIR = CONFIG.getProperty("user-runtime-dir", "/run/user/1000");

    private DevEnvironment() {}

    public static boolean enabled() { return KEY.length() >= 43; }

    private static Properties load() {
        Properties properties = new Properties();
        Path path = Path.of("config", "atlas-dev.properties");
        if (Files.exists(path)) {
            try (var input = Files.newInputStream(path)) { properties.load(input); }
            catch (IOException error) { throw new IllegalStateException("Configuração do lobby dev inválida."); }
        }
        String mode = properties.getProperty("mode", "primary");
        if (!mode.equals("primary") && !mode.equals("development")) {
            throw new IllegalStateException("Modo do lobby dev inválido.");
        }
        if (mode.equals("development") && properties.getProperty("broker-key", "").length() < 43) {
            throw new IllegalStateException("Homologação exige chave própria para o broker.");
        }
        return properties;
    }

    private static int port(String name, int fallback) {
        int value = Integer.parseInt(CONFIG.getProperty(name, Integer.toString(fallback)));
        if (value < 1024 || value > 65535) throw new IllegalStateException("Porta do lobby dev inválida.");
        return value;
    }
}
