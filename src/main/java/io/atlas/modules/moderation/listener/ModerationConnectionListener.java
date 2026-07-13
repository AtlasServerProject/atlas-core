package io.atlas.modules.moderation.listener;

import io.atlas.modules.moderation.service.ModerationService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class ModerationConnectionListener {
    private ModerationConnectionListener() {
    }

    public static void register(ModerationService service) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.player;
            var playerBan = service.activeBan(player.getUUID());
            if (playerBan.isPresent()) {
                player.connection.disconnect(service.disconnectMessage(playerBan.get()));
                return;
            }

            var ipBan = service.activeIpBan(player.getIpAddress());
            ipBan.ifPresent(punishment -> player.connection.disconnect(service.disconnectMessage(punishment)));
        });
    }
}
