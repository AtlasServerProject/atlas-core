package io.atlas.mixin;

import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.auth.service.AuthProtectionService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public class CommandsMixin {

    private static final AuthProtectionService AUTH_PROTECTION =
            AuthModule.getProtectionService();

    @Inject(method = "performPrefixedCommand", at = @At("HEAD"), cancellable = true)
    private void atlas$blockUnauthenticatedCommands(
            CommandSourceStack source,
            String command,
            CallbackInfo callback
    ) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (io.atlas.modules.moderation.ModerationModule.getStaffMode().blocksCommand(player, command)) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("§cSaia do StaffMode com /staffmode off antes de usar comandos de gameplay."));
            callback.cancel();
            return;
        }
        if (io.atlas.modules.moderation.ModerationModule.getStaffTools().blocksCommand(player, command)) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("§cVocê está congelado. Aguarde a staff; o chat continua disponível."));
            callback.cancel();
            return;
        }
        if (AUTH_PROTECTION.canExecuteCommand(player, command)) {
            return;
        }

        source.sendFailure(AUTH_PROTECTION.commandBlockedMessage(player));
        callback.cancel();
    }
    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void atlas$blockFrozenParsedCommand(
            com.mojang.brigadier.ParseResults<CommandSourceStack> parsed,
            String command,
            CallbackInfo callback
    ) {
        CommandSourceStack source = parsed.getContext().getSource();
        if (source.getEntity() instanceof ServerPlayer staffPlayer
                && io.atlas.modules.moderation.ModerationModule.getStaffMode().blocksCommand(staffPlayer, command)) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("§cSaia do StaffMode com /staffmode off antes de usar comandos de gameplay."));
            callback.cancel();
            return;
        }
        if (source.getEntity() instanceof ServerPlayer player
                && io.atlas.modules.moderation.ModerationModule.getStaffTools().blocksCommand(player, command)) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("§cVocê está congelado. Aguarde a staff; o chat continua disponível."));
            callback.cancel();
        }
    }
}
