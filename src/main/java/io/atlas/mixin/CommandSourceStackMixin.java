package io.atlas.mixin;

import io.atlas.modules.rank.RankModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.network.chat.Component;

@Mixin(CommandSourceStack.class)
public class CommandSourceStackMixin {

    @Inject(method = "hasPermission", at = @At("HEAD"), cancellable = true)
    private void atlas$checkMinecraftPermission(
            int requiredLevel,
            CallbackInfoReturnable<Boolean> callback
    ) {
        CommandSourceStack source = (CommandSourceStack) (Object) this;
        ServerPlayer player = source.getPlayer();

        if (player != null && RankModule.getRankService()
                .hasPermission(player.getUUID(), "minecraft.*")) {
            callback.setReturnValue(true);
        }
    }

    @Redirect(
            method = "broadcastToAdmins",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;sendSystemMessage(Lnet/minecraft/network/chat/Component;)V"
            )
    )
    private void atlas$keepPlayerCommandFeedbackPrivate(
            ServerPlayer recipient,
            Component message
    ) {
        CommandSourceStack source = (CommandSourceStack) (Object) this;

        if (source.getPlayer() == null) {
            recipient.sendSystemMessage(message);
        }
    }
}
