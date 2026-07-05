package io.atlas;

import io.atlas.bootstrap.AtlasBootstrap;
import io.atlas.commands.BalanceCommand;
import io.atlas.commands.AtlasCommand;
import io.atlas.commands.AddMoneyCommand;
import io.atlas.commands.RankCommand;
import io.atlas.commands.RegisterCommand;
import io.atlas.commands.LoginCommand;
import io.atlas.commands.LogoutCommand;
import io.atlas.commands.SpawnCommand;
import io.atlas.commands.LobbyCommand;
import io.atlas.commands.RandomTeleportCommand;
import io.atlas.commands.HomeCommand;
import io.atlas.commands.SetHomeCommand;
import io.atlas.commands.DeleteHomeCommand;
import io.atlas.commands.HomesCommand;
import io.atlas.commands.ClaimCommands;
import io.atlas.commands.TrashCommand;
import io.atlas.commands.DroppedItemsCommand;
import io.atlas.commands.EndBattleCommand;
import io.atlas.commands.PayCommand;
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
    RankCommand.register(dispatcher);
    RegisterCommand.register(dispatcher);
    LoginCommand.register(dispatcher);
    LogoutCommand.register(dispatcher);
    SpawnCommand.register(dispatcher);
    LobbyCommand.register(dispatcher);
    RandomTeleportCommand.register(dispatcher);
    HomeCommand.register(dispatcher);
    SetHomeCommand.register(dispatcher);
    DeleteHomeCommand.register(dispatcher);
    HomesCommand.register(dispatcher);
    ClaimCommands.register(dispatcher);
    TrashCommand.register(dispatcher);
    DroppedItemsCommand.register(dispatcher);
    EndBattleCommand.register(dispatcher);
    PayCommand.register(dispatcher);
});

        LOGGER.info("Comando /atlas registrado.");
    }
}
