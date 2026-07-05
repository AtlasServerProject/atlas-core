package io.atlas.modules.lobby.listener;

import io.atlas.modules.lobby.service.LobbyNpcService;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;

public class LobbyNpcListener {

    public static void register(LobbyNpcService npcService) {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            return npcService.interact(serverPlayer, world, hand, entity);
        });
    }
}
