package io.atlas.mixin;

import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.auth.service.AuthGameplayProtectionService;
import io.atlas.modules.lobby.LobbyModule;
import io.atlas.modules.lobby.service.ServerSelectorService;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

    private static final AuthGameplayProtectionService AUTH_GAMEPLAY_PROTECTION =
            AuthModule.getGameplayProtectionService();
    private static final ServerSelectorService SERVER_SELECTOR =
            LobbyModule.getSelectorService();

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void atlas$blockInventoryClick(
            ServerboundContainerClickPacket packet,
            CallbackInfo callback
    ) {
        if (SERVER_SELECTOR.shouldBlockInventoryClick(
                player,
                packet.getCarriedItem(),
                packet.getChangedSlots().values(),
                packet.getSlotNum()
        )) {
            callback.cancel();
            return;
        }
        cancelWhenUnauthenticated(callback);
    }

    @Inject(method = "handleContainerButtonClick", at = @At("HEAD"), cancellable = true)
    private void atlas$blockInventoryButton(
            ServerboundContainerButtonClickPacket packet,
            CallbackInfo callback
    ) {
        cancelWhenUnauthenticated(callback);
    }

    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"), cancellable = true)
    private void atlas$blockRecipePlacement(
            ServerboundPlaceRecipePacket packet,
            CallbackInfo callback
    ) {
        cancelWhenUnauthenticated(callback);
    }

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"), cancellable = true)
    private void atlas$blockCreativeInventory(
            ServerboundSetCreativeModeSlotPacket packet,
            CallbackInfo callback
    ) {
        if (SERVER_SELECTOR.shouldBlockCreativeSlot(
                player,
                packet.slotNum(),
                packet.itemStack()
        )) {
            callback.cancel();
            return;
        }
        cancelWhenUnauthenticated(callback);
    }

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void atlas$blockSelectorDrop(
            ServerboundPlayerActionPacket packet,
            CallbackInfo callback
    ) {
        boolean protectedAction = packet.getAction()
                == ServerboundPlayerActionPacket.Action.DROP_ITEM
                || packet.getAction() == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS
                || packet.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND;
        if (protectedAction && SERVER_SELECTOR.isSelector(player.getMainHandItem())) {
            callback.cancel();
        }
    }

    private void cancelWhenUnauthenticated(CallbackInfo callback) {
        if (!AUTH_GAMEPLAY_PROTECTION.canUseInventory(player)) {
            callback.cancel();
        }
    }
}
