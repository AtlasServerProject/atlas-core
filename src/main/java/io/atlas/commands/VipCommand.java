package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.vip.VipModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class VipCommand {
    private VipCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("vip").requires(CommandSourceStack::isPlayer)
                .executes(context -> {
                    var source = context.getSource();
                    var player = source.getPlayerOrException();
                    if (!AuthModule.getAuthService().isAuthenticated(player.getUUID())) {
                        source.sendFailure(Component.literal("§cFaça login antes de consultar seu VIP."));
                        return 0;
                    }
                    try {
                        VipModule.service().status(player.getUUID()).forEach(line ->
                                source.sendSuccess(() -> Component.literal(line), false));
                        return 1;
                    } catch (Exception exception) {
                        source.sendFailure(Component.literal("§cNão foi possível consultar seu VIP. Tente novamente em instantes."));
                        return 0;
                    }
                }));
    }
}
