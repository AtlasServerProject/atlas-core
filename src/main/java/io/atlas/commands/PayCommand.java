package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import io.atlas.modules.economy.EconomyService;
import io.atlas.modules.player.listener.PlayerJoinListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigInteger;

public class PayCommand {

    private static final EconomyService economyService = new EconomyService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("pay")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("valor", LongArgumentType.longArg(1))
                                        .executes(context -> pay(
                                                context.getSource().getPlayerOrException(),
                                                EntityArgument.getPlayer(context, "player"),
                                                BigInteger.valueOf(LongArgumentType.getLong(context, "valor"))
                                        ))))
        );
    }

    private static int pay(ServerPlayer sender, ServerPlayer target, BigInteger amount) {
        if (sender.getUUID().equals(target.getUUID())) {
            sender.sendSystemMessage(Component.literal("§cVocê não pode pagar a si mesmo."));
            return 0;
        }

        if (PlayerJoinListener.getPlayerService().getProfile(sender.getUUID()).isEmpty()) {
            sender.sendSystemMessage(Component.literal("§cSeu perfil ainda não foi carregado."));
            return 0;
        }

        if (PlayerJoinListener.getPlayerService().getProfile(target.getUUID()).isEmpty()) {
            sender.sendSystemMessage(Component.literal("§cO perfil desse jogador ainda não foi carregado."));
            return 0;
        }

        if (!economyService.withdraw(sender, amount)) {
            sender.sendSystemMessage(Component.literal("§cSaldo insuficiente para enviar §f"
                    + EconomyService.format(amount) + "§c CobbleDollars."));
            return 0;
        }

        economyService.deposit(target, amount);

        String formattedAmount = EconomyService.format(amount);
        sender.sendSystemMessage(Component.literal("§aVocê enviou §f" + formattedAmount
                + "§a CobbleDollars para §f" + target.getGameProfile().getName() + "§a."));
        target.sendSystemMessage(Component.literal("§aVocê recebeu §f" + formattedAmount
                + "§a CobbleDollars de §f" + sender.getGameProfile().getName() + "§a."));

        return 1;
    }
}
