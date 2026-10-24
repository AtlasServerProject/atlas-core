package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rules.RulesModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class RulesCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("regras")
            .requires(source -> source.isPlayer() && AuthModule.getAuthService().isAuthenticated(source.getPlayer().getUUID()))
            .executes(context -> RulesModule.service().show(context.getSource().getPlayerOrException()))
            .then(Commands.literal("aceitar").executes(context -> RulesModule.service().accept(context.getSource().getPlayerOrException()))));
    }
}
