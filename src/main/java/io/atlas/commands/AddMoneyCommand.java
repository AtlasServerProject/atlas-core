package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import io.atlas.modules.economy.EconomyService;
import io.atlas.modules.player.listener.PlayerJoinListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import io.atlas.permission.Permission;

public class AddMoneyCommand {

    private static final EconomyService economyService = new EconomyService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("addmoney")
                        .then(Commands.argument("valor", DoubleArgumentType.doubleArg(0.01))
                                .executes(context -> {
                                    var player = context.getSource().getPlayerOrException();
                                    double amount = DoubleArgumentType.getDouble(context, "valor");

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

                                    economyService.deposit(profile.get(), amount);

                                    context.getSource().sendSuccess(
                                            () -> Component.literal("§aVocê recebeu §f" + amount + "§a coins."),
                                            false
                                    );

                                    return 1;
                                }))
        );
    }
}