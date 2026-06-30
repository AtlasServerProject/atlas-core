package io.atlas.bootstrap;

import io.atlas.AtlasMod;
import io.atlas.module.ModuleManager;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.admin.AdminModule;
import io.atlas.modules.database.DatabaseModule;
import io.atlas.modules.chat.ChatModule;
import io.atlas.modules.economy.EconomyModule;
import io.atlas.modules.player.PlayerModule;
import io.atlas.modules.rank.RankModule;

public class AtlasBootstrap {

    private static final ModuleManager moduleManager = new ModuleManager();

    public static void start() {

        AtlasMod.LOGGER.info("========================================");
        AtlasMod.LOGGER.info("           Atlas Core");
        AtlasMod.LOGGER.info("========================================");

        // Registro dos módulos
        moduleManager.register(new DatabaseModule());
        moduleManager.register(new RankModule());
        moduleManager.register(new ChatModule());
        moduleManager.register(new EconomyModule());
        moduleManager.register(new PlayerModule());
        moduleManager.register(new AuthModule());
        moduleManager.register(new AdminModule());

        // Inicialização
        moduleManager.enableModules();

        AtlasMod.LOGGER.info("========================================");
        AtlasMod.LOGGER.info("Atlas Core iniciado com sucesso!");
        AtlasMod.LOGGER.info("========================================");
    }

    public static void shutdown() {
        moduleManager.disableModules();
    }
}
