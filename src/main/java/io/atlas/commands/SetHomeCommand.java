package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.home.HomeModule;
import io.atlas.modules.home.service.HomeService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class SetHomeCommand {

    private SetHomeCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sethome")
                .requires(CommandSourceStack::isPlayer)
                .then(Commands.argument("nome", StringArgumentType.word())
                        .executes(context -> execute(
                                context.getSource(),
                                StringArgumentType.getString(context, "nome")
                        ))));
    }

    private static int execute(CommandSourceStack source, String name)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        HomeService service = HomeModule.getHomeService();
        HomeService.SetResult result = service.setHome(player, name);
        Component message = switch (result) {
            case CREATED -> Component.literal("§aHome §f" + name + " §acriada. §7("
                    + service.listHomes(player.getUUID()).size() + "/"
                    + service.homeLimit(player.getUUID()) + ")");
            case UPDATED -> Component.literal("§aHome §f" + name + " §aatualizada.");
            case INVALID_NAME -> Component.literal(
                    "§cUse um nome de 1 a 16 caracteres: letras, números, _ ou -."
            );
            case LIMIT_REACHED -> Component.literal(
                    "§cVocê atingiu seu limite de " + service.homeLimit(player.getUUID()) + " homes."
            );
            case SURVIVAL_ONLY -> Component.literal(
                    "§cHomes só podem ser criadas dentro do Survival Emerald."
            );
        };
        if (result == HomeService.SetResult.CREATED || result == HomeService.SetResult.UPDATED) {
            source.sendSuccess(() -> message, false);
            return 1;
        }
        source.sendFailure(message);
        return 0;
    }
}
