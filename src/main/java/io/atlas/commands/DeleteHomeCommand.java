package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.home.HomeModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class DeleteHomeCommand {

    private DeleteHomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("delhome")
                .requires(CommandSourceStack::isPlayer)
                .then(Commands.argument("nome", StringArgumentType.word())
                        .executes(context -> execute(
                                context.getSource(),
                                StringArgumentType.getString(context, "nome")
                        ))));
    }

    private static int execute(CommandSourceStack source, String name)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        boolean deleted = HomeModule.getHomeService()
                .deleteHome(source.getPlayerOrException().getUUID(), name);
        if (!deleted) {
            source.sendFailure(Component.literal("§cA home §f" + name + " §cnão existe."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("§aHome §f" + name + " §aremovida."), false);
        return 1;
    }
}
