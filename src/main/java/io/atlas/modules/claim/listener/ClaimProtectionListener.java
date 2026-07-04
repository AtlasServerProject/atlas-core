package io.atlas.modules.claim.listener;

import io.atlas.modules.claim.model.TrustLevel;
import io.atlas.modules.claim.service.ClaimService;
import io.atlas.modules.lobby.service.LobbyWorlds;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
            TrustLevel required = stack.getItem() instanceof BlockItem || isWorldChangingItem(stack)
                    ? TrustLevel.BUILD
                    : (world.getBlockEntity(hit.getBlockPos()) != null ? TrustLevel.CONTAINER : TrustLevel.ACCESS);
            return service.can(serverPlayer, hit.getBlockPos(), required, true)
                    ? InteractionResult.PASS : InteractionResult.FAIL;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !LobbyWorlds.isSurvivalEmerald(world)) {
                return InteractionResult.PASS;
            }
            return service.can(serverPlayer, entity.blockPosition(), TrustLevel.BUILD, true)
                    ? InteractionResult.PASS : InteractionResult.FAIL;
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || !LobbyWorlds.isSurvivalEmerald(world)) {
                return InteractionResult.PASS;
            }
            return service.can(serverPlayer, entity.blockPosition(), TrustLevel.ACCESS, true)
                    ? InteractionResult.PASS : InteractionResult.FAIL;
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(source.getEntity() instanceof ServerPlayer attacker)
                    || !LobbyWorlds.isSurvivalEmerald(entity.level())) return true;
            return service.can(attacker, entity.blockPosition(), TrustLevel.BUILD, true);
        });
    }

    private static boolean isWorldChangingItem(net.minecraft.world.item.ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(Items.LAVA_BUCKET)
                || stack.is(Items.POWDER_SNOW_BUCKET) || stack.is(Items.FLINT_AND_STEEL)
                || stack.is(Items.FIRE_CHARGE);
    }
}
