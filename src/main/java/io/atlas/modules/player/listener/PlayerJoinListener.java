package io.atlas.modules.player.listener;

import io.atlas.modules.player.service.PlayerService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class PlayerJoinListener {

    public static PlayerService getPlayerService() {
    return playerService;
}

    private static final PlayerService playerService = new PlayerService();

    public static void register() {

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            playerService.loadPlayer(
                    handler.player.getUUID(),
                    handler.player.getName().getString()
            );
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            playerService.unloadPlayer(
                    handler.player.getUUID()
            );
        });
    }
}