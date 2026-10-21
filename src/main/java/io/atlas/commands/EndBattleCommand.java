package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.atlas.modules.battle.BattleModule;
import io.atlas.modules.battle.service.BattleEndService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class EndBattleCommand {

    private static final BattleEndService battleEndService = BattleModule.getBattleEndService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("endbattle")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> execute(context.getSource()))
        );
    }

    private static int execute(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (battleEndService.endBattle(player)) {
            source.sendSuccess(
                    () -> Component.literal("§aBatalha encerrada com sucesso."),
                    false
            );
            return 1;
        }

        source.sendFailure(Component.literal("§cVocê não está em uma batalha ativa."));
        return 0;
    }
}
