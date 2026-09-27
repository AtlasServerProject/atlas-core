package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.survival.SurvivalModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class BackCommand {

    private BackCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("back")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> execute(context.getSource()))
        );
    }

    private static int execute(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (!AuthModule.getAuthService().isAuthenticated(player.getUUID())) {
            source.sendFailure(Component.literal("§cFaça login antes de usar /back."));
            return 0;
        }

        return SurvivalModule.getBackService().back(player) ? 1 : 0;
    }
}
