package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.home.HomeModule;
import io.atlas.modules.home.service.HomeService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class HomeCommand {

    private HomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("home")
                .requires(CommandSourceStack::isPlayer)
                .executes(context -> execute(context.getSource(), null))
                .then(Commands.argument("nome", StringArgumentType.word())
                        .executes(context -> execute(
                                context.getSource(),
                                StringArgumentType.getString(context, "nome")
                        ))));
    }

    private static int execute(CommandSourceStack source, String name)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        HomeService.TeleportResult result = HomeModule.getHomeService()
                .requestTeleport(source.getPlayerOrException(), name);
        Component message = switch (result) {
            case NOT_FOUND -> Component.literal(name == null
                    ? "§cVocê ainda não possui nenhuma home."
                    : "§cA home §f" + name + " §cnão existe.");
            case ALREADY_PENDING -> Component.literal("§eVocê já possui um teleporte em andamento.");
            case COOLDOWN -> Component.literal("§eO comando ainda está em cooldown.");
            case STARTED -> Component.empty();
        };
        if (result == HomeService.TeleportResult.STARTED) {
            return 1;
        }
        source.sendFailure(message);
        return 0;
    }
}
