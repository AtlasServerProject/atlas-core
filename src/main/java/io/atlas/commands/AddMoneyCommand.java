package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import io.atlas.modules.economy.EconomyService;
import io.atlas.modules.player.listener.PlayerJoinListener;
import io.atlas.permission.Permission;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.math.BigInteger;

public class AddMoneyCommand {

    private static final EconomyService economyService = new EconomyService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("addmoney")
                        .then(Commands.argument("valor", LongArgumentType.longArg(1))
                                .executes(context -> {
                                    var player = context.getSource().getPlayerOrException();
                                    BigInteger amount = BigInteger.valueOf(LongArgumentType.getLong(context, "valor"));

                                    var profile = PlayerJoinListener.getPlayerService()
                                            .getProfile(player.getUUID());

                                    if (!Permission.check(player, "atlas.economy.addmoney")) {
                                        Permission.deny(player);
                                        return 0;
                                    }

                                    if (profile.isEmpty()) {
                                        context.getSource().sendFailure(
                                                Component.literal("§cSeu perfil ainda não foi carregado.")
                                        );
                                        return 0;
                                    }

                                    economyService.deposit(player, amount);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("§aVocê recebeu §f" + EconomyService.format(amount) + "§a CobbleDollars."),
                                            false
                                    );

                                    return 1;
                                }))
        );
    }
}
