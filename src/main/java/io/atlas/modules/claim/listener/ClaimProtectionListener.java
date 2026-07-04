package io.atlas.modules.claim.listener;

import io.atlas.modules.claim.model.TrustLevel;
import io.atlas.modules.claim.service.ClaimService;
import io.atlas.modules.lobby.service.LobbyWorlds;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;

public final class ClaimProtectionListener {
    private ClaimProtectionListener() {}

    public static void register(ClaimService service) {
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) ->
                !(player instanceof ServerPlayer serverPlayer)
                        || !LobbyWorlds.isSurvivalEmerald(world)
                        || service.can(serverPlayer, pos, TrustLevel.BUILD, true));

        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !LobbyWorlds.isSurvivalEmerald(world)) {
                return InteractionResult.PASS;
            }
            var stack = player.getItemInHand(hand);
            if (stack.is(Items.GOLDEN_SHOVEL)) {
                service.select(serverPlayer, hit.getBlockPos());
                return InteractionResult.SUCCESS;
            }
            if (stack.is(Items.STICK)) {
                service.inspect(serverPlayer, hit.getBlockPos());
                return InteractionResult.SUCCESS;
            }
            TrustLevel required = stack.getItem() instanceof BlockItem
                    ? TrustLevel.BUILD
                    : (world.getBlockEntity(hit.getBlockPos()) != null ? TrustLevel.CONTAINER : TrustLevel.ACCESS);
            return service.can(serverPlayer, hit.getBlockPos(), required, true)
                    ? InteractionResult.PASS : InteractionResult.FAIL;
        });
    }
}
