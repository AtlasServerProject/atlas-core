package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.atlas.modules.rank.RankModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;

public class DevCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("dev")
                        .executes(context -> enableDevMode(context.getSource()))
        );
    }

    private static int enableDevMode(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!RankModule.getRankService().canManageRanks(player.getUUID())) {
            source.sendFailure(Component.literal("§cO modo dev é restrito a Dono e ADM."));
            return 0;
        }

        PlayerList playerList = source.getServer().getPlayerList();
        if (!playerList.isOp(player.getGameProfile())) {
            playerList.op(player.getGameProfile());
        }

        source.sendSuccess(
                () -> Component.literal("§aModo dev ativado. §7Você agora possui acesso vanilla ao /op."),
                false
        );
        return 1;
    }
}
