package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.auth.service.AuthService;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.survival.SurvivalModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class RandomTeleportCommand {

    private static final AuthService AUTH_SERVICE = AuthModule.getAuthService();
    private RandomTeleportCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("rtp")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> execute(context.getSource()))
        );
    }

    private static int execute(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (!AUTH_SERVICE.isAuthenticated(player.getUUID())) {
            source.sendFailure(Component.literal("§cFaça login antes de usar /rtp."));
            return 0;
        }
        if (!LobbyWorlds.isEmerald(player.level())
                && !LobbyWorlds.isSurvivalEmerald(player.level())) {
            source.sendFailure(Component.literal(
                    "§eO /rtp está disponível somente no Lobby Emerald ou Survival Emerald."
            ));
            return 0;
        }

        return SurvivalModule.getRandomTeleportService().request(player) ? 1 : 0;
    }
}
