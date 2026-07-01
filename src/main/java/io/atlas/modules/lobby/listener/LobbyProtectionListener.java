package io.atlas.modules.lobby.listener;

import io.atlas.modules.lobby.service.LobbyProtectionService;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;

public class LobbyProtectionListener {

    public static void register(LobbyProtectionService protectionService) {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
                protectionService.canModify(player, world)
        );

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player.getItemInHand(hand).getItem() instanceof BlockItem)) {
                return InteractionResult.PASS;
            }

            return protectionService.canModify(player, world)
                    ? InteractionResult.PASS
                    : InteractionResult.FAIL;
        });
    }
}
