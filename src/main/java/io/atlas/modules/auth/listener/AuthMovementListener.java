package io.atlas.modules.auth.listener;

import io.atlas.modules.auth.service.AuthMovementLockService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class AuthMovementListener {

    public static void register(AuthMovementLockService movementLockService) {
        ServerTickEvents.END_SERVER_TICK.register(movementLockService::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                movementLockService.remove(handler.player.getUUID())
        );
    }
}
