package io.atlas;

import io.atlas.bootstrap.AtlasBootstrap;
import io.atlas.commands.BalanceCommand;
import io.atlas.commands.AtlasCommand;
import io.atlas.commands.AddMoneyCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AtlasMod implements ModInitializer {

    public static final String MOD_ID = "atlas-core";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        AtlasBootstrap.start();

CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
    AtlasCommand.register(dispatcher);
    BalanceCommand.register(dispatcher);
    AddMoneyCommand.register(dispatcher);
});

        LOGGER.info("Comando /atlas registrado.");
    }
}