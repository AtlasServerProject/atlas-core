package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rank.RankModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;

import java.util.Locale;
import java.util.Set;

public final class EcCommand {

    private static final String EC_PERMISSION = "atlas.ec";
    private static final Set<String> EC_RANKS = Set.of(
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

    private EcCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ec")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> open(context.getSource()))
        );

        dispatcher.register(
                Commands.literal("enderchest")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> open(context.getSource()))
        );
    }

    private static int open(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!AuthModule.getAuthService().isAuthenticated(player.getUUID())) {
            source.sendFailure(Component.literal("§cFaça login antes de usar /ec."));
            return 0;
        }

        if (!canUseEc(player)) {
            source.sendFailure(Component.literal("§cVocê não tem permissão para usar /ec."));
            return 0;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> ChestMenu.threeRows(
                        containerId,
                        inventory,
                        player.getEnderChestInventory()
                ),
                Component.literal("§5Ender Chest")
        ));
        return 1;
    }

    private static boolean canUseEc(ServerPlayer player) {
        var rankService = RankModule.getRankService();
        return rankService.hasPermission(player.getUUID(), EC_PERMISSION)
                || rankService.getPlayerRanks(player.getUUID()).stream()
                .map(rank -> rank.getIdentifier().toUpperCase(Locale.ROOT))
                .anyMatch(EC_RANKS::contains);
    }
}
