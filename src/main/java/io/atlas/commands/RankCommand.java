package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.rank.RankModule;
import io.atlas.modules.rank.service.PlayerDisplayService;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Set;

public class RankCommand {

    private static final Set<String> CONSOLE_NAMES = Set.of("server", "rcon");
    private static final RankService rankService = RankModule.getRankService();
    private static final PlayerDisplayService displayService = RankModule.getDisplayService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("rank")
                        .requires(RankCommand::canUse)
                        .then(Commands.literal("set")
                                .then(Commands.argument("jogador", StringArgumentType.word())
                                        .then(Commands.argument("cargo", StringArgumentType.word())
                                                .executes(context -> setRank(
                                                        context.getSource(),
                                                        StringArgumentType.getString(context, "jogador"),
                                                        StringArgumentType.getString(context, "cargo")
                                                )))))
        );
    }

    private static boolean canUse(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            return rankService.canManageRanks(player.getUUID());
        }

        return CONSOLE_NAMES.contains(source.getTextName().toLowerCase(Locale.ROOT));
    }

    private static int setRank(CommandSourceStack source, String username, String rankIdentifier) {
        RankService.RankAssignmentResult result = rankService.assignRank(username, rankIdentifier);

        if (result == RankService.RankAssignmentResult.RANK_NOT_FOUND) {
            source.sendFailure(Component.literal("§cO cargo informado não existe."));
            return 0;
        }

        if (result == RankService.RankAssignmentResult.PLAYER_NOT_FOUND) {
            source.sendFailure(Component.literal("§cO jogador informado ainda não está cadastrado."));
            return 0;
        }

        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(username);
        if (target != null) {
            displayService.updatePlayer(target);
        }

        source.sendSuccess(
                () -> Component.literal(
                        "§aCargo §f" + rankIdentifier.toUpperCase(Locale.ROOT)
                                + " §aatribuído a §f" + username + "§a."
                ),
                true
        );
        return 1;
    }
}
