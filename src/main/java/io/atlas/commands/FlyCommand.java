package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rank.RankModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;


public final class FlyCommand {

    private FlyCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("fly")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> toggle(context.getSource()))
        );
    }

    private static int toggle(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!AuthModule.getAuthService().isAuthenticated(player.getUUID())) {
            source.sendFailure(Component.literal("§cFaça login antes de usar /fly."));
            return 0;
        }

        if (!canUseFly(player)) {
            source.sendFailure(Component.literal("§cVocê não tem permissão para usar /fly."));
            return 0;
        }

        boolean enable = !player.getAbilities().mayfly;
        player.getAbilities().mayfly = enable;
        if (!enable && !keepsFlightByGameMode(player)) {
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();

        source.sendSuccess(
                () -> Component.literal(enable
                        ? "§aFly ativado."
                        : "§eFly desativado."),
                false
        );
        return 1;
    }

    public static boolean canUseFly(ServerPlayer player) {
        return RankModule.getRankService().canFly(player.getUUID());
    }

    private static boolean keepsFlightByGameMode(ServerPlayer player) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        return mode == GameType.CREATIVE || mode == GameType.SPECTATOR;
    }
}
