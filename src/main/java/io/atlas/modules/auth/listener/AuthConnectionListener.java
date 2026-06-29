package io.atlas.modules.auth.listener;

import io.atlas.modules.auth.service.AuthService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class AuthConnectionListener {

    public static void register(AuthService authService) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                authService.loadPlayer(
                        handler.player.getUUID(),
                        handler.player.getIpAddress()
                )
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                authService.unloadPlayer(handler.player.getUUID())
        );
    }
}
