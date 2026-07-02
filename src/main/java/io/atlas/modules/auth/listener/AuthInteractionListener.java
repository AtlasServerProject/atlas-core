package io.atlas.modules.auth.listener;

import io.atlas.modules.auth.service.AuthGameplayProtectionService;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;

public class AuthInteractionListener {

    public static void register(AuthGameplayProtectionService protectionService) {
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                protectionService.canInteract(player)
                        ? InteractionResult.PASS
                        : InteractionResult.FAIL
        );

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                protectionService.canInteract(player)
                        ? InteractionResult.PASS
                        : InteractionResult.FAIL
        );

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) ->
                protectionService.canInteract(player)
                        ? InteractionResult.PASS
                        : InteractionResult.FAIL
        );

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                protectionService.canInteract(player)
                        ? InteractionResult.PASS
                        : InteractionResult.FAIL
        );

        UseItemCallback.EVENT.register((player, world, hand) ->
                protectionService.canInteract(player)
                        ? InteractionResultHolder.pass(player.getItemInHand(hand))
                        : InteractionResultHolder.fail(player.getItemInHand(hand))
        );
    }
}
