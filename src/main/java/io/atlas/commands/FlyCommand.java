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

import java.util.Locale;
import java.util.Set;

public final class FlyCommand {

    private static final String FLY_PERMISSION = "atlas.fly";
    private static final Set<String> FLY_RANKS = Set.of(
            "VIP",
            "VIP+",
            "VIPPLUS",
            "VIP++",
            "VIPPLUSPLUS",
            "SUP",
            "SUPPORT",
            "MOD",
            "MODERATOR",
            "ADM",
            "ADMIN",
            "DONO",
            "OWNER"
    );

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

    private static boolean canUseFly(ServerPlayer player) {
        var rankService = RankModule.getRankService();
        return rankService.hasPermission(player.getUUID(), FLY_PERMISSION)
                || rankService.getPlayerRanks(player.getUUID()).stream()
                .map(rank -> rank.getIdentifier().toUpperCase(Locale.ROOT))
                .anyMatch(FLY_RANKS::contains);
    }

    private static boolean keepsFlightByGameMode(ServerPlayer player) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        return mode == GameType.CREATIVE || mode == GameType.SPECTATOR;
    }
}
