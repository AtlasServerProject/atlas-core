package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.rank.RankModule;
import io.atlas.modules.rank.model.Rank;
import io.atlas.modules.rank.service.PlayerDisplayService;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
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
                                .then(playerArgument((source, username, rank) ->
                                        changePlayerRank(source, username, rank, true))))
                        .then(Commands.literal("remove")
                                .then(playerArgument((source, username, rank) ->
                                        changePlayerRank(source, username, rank, false))))
                        .then(Commands.literal("list")
                                .executes(context -> listRanks(context.getSource())))
                        .then(Commands.literal("info")
                                .then(Commands.argument("jogador", StringArgumentType.word())
                                        .executes(context -> showPlayerInfo(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "jogador")
                                        ))))
                        .then(Commands.literal("reload")
                                .executes(context -> reload(context.getSource())))
                        .then(Commands.literal("permission")
                                .then(permissionCommand("add", true))
                                .then(permissionCommand("remove", false)))
        );
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String>
    playerArgument(RankChangeExecutor executor) {
        return Commands.argument("jogador", StringArgumentType.word())
                .then(Commands.argument("cargo", StringArgumentType.word())
                        .executes(context -> executor.execute(
                                context.getSource(),
                                StringArgumentType.getString(context, "jogador"),
                                StringArgumentType.getString(context, "cargo")
                        )));
    }

    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack>
    permissionCommand(String name, boolean add) {
        return Commands.literal(name)
                .then(Commands.argument("cargo", StringArgumentType.word())
                        .then(Commands.argument("permissao", StringArgumentType.word())
                                .executes(context -> changePermission(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "cargo"),
                                        StringArgumentType.getString(context, "permissao"),
                                        add
                                ))));
    }

    private static boolean canUse(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player != null) {
            return rankService.canManageRanks(player.getUUID());
        }

        return CONSOLE_NAMES.contains(source.getTextName().toLowerCase(Locale.ROOT));
    }

    private static int changePlayerRank(
            CommandSourceStack source,
            String username,
            String rankIdentifier,
            boolean assign
    ) {
        RankService.RankAssignmentResult result = assign
                ? rankService.assignRank(username, rankIdentifier)
                : rankService.removeRank(username, rankIdentifier);

        if (!handleMutationFailure(source, result)) {
            return 0;
        }

        ServerPlayer target = source.getServer().getPlayerList().getPlayerByName(username);
        if (target != null) {
            displayService.updatePlayer(target);
        }

        String action = assign ? "atribuído a" : "removido de";
        source.sendSuccess(
                () -> Component.literal(
                        "§aCargo §f" + rankIdentifier.toUpperCase(Locale.ROOT)
                                + " §a" + action + " §f" + username + "§a."
                ),
                true
        );
        return 1;
    }

    private static int listRanks(CommandSourceStack source) {
        List<Rank> ranks = rankService.getAllRanks();
        String values = ranks.stream()
                .map(rank -> rank.getIdentifier() + " (" + rank.getPriority() + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("nenhum");

        source.sendSuccess(() -> Component.literal("§6Cargos: §f" + values), false);
        return ranks.size();
    }

    private static int showPlayerInfo(CommandSourceStack source, String username) {
        var info = rankService.getPlayerRankInfo(username);
        if (info.isEmpty()) {
            source.sendFailure(Component.literal("§cO jogador informado ainda não está cadastrado."));
            return 0;
        }

        String values = info.get().ranks().stream()
                .map(Rank::getIdentifier)
                .reduce((left, right) -> left + ", " + right)
                .orElse("nenhum");
        source.sendSuccess(
                () -> Component.literal("§6Cargos de §f" + username + "§6: §f" + values),
                false
        );
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        rankService.loadRanks();
        source.getServer().getPlayerList().getPlayers().forEach(displayService::updatePlayer);
        source.sendSuccess(() -> Component.literal("§aRanks e permissões recarregados."), false);
        return 1;
    }

    private static int changePermission(
            CommandSourceStack source,
            String rankIdentifier,
            String permission,
            boolean add
    ) {
        RankService.RankAssignmentResult result = add
                ? rankService.addPermission(rankIdentifier, permission)
                : rankService.removePermission(rankIdentifier, permission);

        if (!handleMutationFailure(source, result)) {
            return 0;
        }

        String action = add ? "adicionada ao" : "removida do";
        source.sendSuccess(
                () -> Component.literal(
                        "§aPermissão §f" + permission + " §a" + action
                                + " cargo §f" + rankIdentifier.toUpperCase(Locale.ROOT) + "§a."
                ),
                true
        );
        return 1;
    }

    private static boolean handleMutationFailure(
            CommandSourceStack source,
            RankService.RankAssignmentResult result
    ) {
        if (result == RankService.RankAssignmentResult.RANK_NOT_FOUND) {
            source.sendFailure(Component.literal("§cO cargo informado não existe."));
            return false;
        }

        if (result == RankService.RankAssignmentResult.PLAYER_NOT_FOUND) {
            source.sendFailure(Component.literal("§cO jogador informado ainda não está cadastrado."));
            return false;
        }

        return true;
    }

    @FunctionalInterface
    private interface RankChangeExecutor {
        int execute(CommandSourceStack source, String username, String rankIdentifier);
    }
}
