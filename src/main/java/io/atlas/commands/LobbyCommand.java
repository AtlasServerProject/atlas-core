package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.auth.service.AuthService;
import io.atlas.modules.lobby.service.LobbyTravelService;
import io.atlas.modules.lobby.service.LobbyWorlds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class LobbyCommand {

    private static final AuthService authService = AuthModule.getAuthService();
    private static final LobbyTravelService travelService = new LobbyTravelService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("lobby")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> execute(context.getSource()))
        );
    }

    private static int execute(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (!authService.isAuthenticated(player.getUUID())) {
            source.sendFailure(Component.literal("§cFaça login antes de usar /lobby."));
            return 0;
        }
        if (!LobbyWorlds.isSurvivalEmerald(player.level())) {
            source.sendFailure(Component.literal(
                    "§eVocê só pode usar /lobby dentro do Survival Emerald."
            ));
            return 0;
        }
        if (!travelService.teleportToEmerald(player)) {
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("§aVocê voltou ao Lobby Emerald."),
                false
        );
        return 1;
    }
}
