package io.atlas.modules.lobby.listener;

import io.atlas.modules.lobby.service.ServerSelectorService;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

public class ServerSelectorListener {

    public static void register(ServerSelectorService selectorService) {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (!selectorService.isSelector(stack)) {
                return InteractionResultHolder.pass(stack);
            }

            if (player instanceof ServerPlayer serverPlayer) {
                selectorService.openSelector(serverPlayer);
            }
            return InteractionResultHolder.success(stack);
        });
    }
}
