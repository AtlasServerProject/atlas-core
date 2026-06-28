package io.atlas.modules.player.listener;

import io.atlas.modules.player.service.PlayerService;
import io.atlas.modules.rank.RankModule;
import io.atlas.modules.rank.service.PlayerDisplayService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class PlayerJoinListener {

    public static PlayerService getPlayerService() {
    return playerService;
}

    private static final PlayerService playerService = new PlayerService();
    private static final PlayerDisplayService displayService = RankModule.getDisplayService();

    public static void register() {

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            playerService.loadPlayer(
                    handler.player.getUUID(),
                    handler.player.getName().getString()
            );
            displayService.updatePlayer(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playerService.unloadPlayer(
                    handler.player.getUUID()
            );
        });
    }
}
