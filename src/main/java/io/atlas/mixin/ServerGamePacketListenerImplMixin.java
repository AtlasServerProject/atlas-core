package io.atlas.mixin;

import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.auth.service.AuthGameplayProtectionService;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
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

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void atlas$blockInventoryClick(
            ServerboundContainerClickPacket packet,
            CallbackInfo callback
    ) {
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
        cancelWhenUnauthenticated(callback);
    }

    private void cancelWhenUnauthenticated(CallbackInfo callback) {
        if (!AUTH_GAMEPLAY_PROTECTION.canUseInventory(player)) {
            callback.cancel();
        }
    }
}
