package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.player.listener.PlayerJoinListener;
import io.atlas.modules.player.model.PlayerProfile;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import io.atlas.modules.economy.EconomyService;

import java.util.Optional;

public class BalanceCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("saldo")
                        .executes(context -> {

                            var player = context.getSource().getPlayerOrException();

                            Optional<PlayerProfile> profile =
                                    PlayerJoinListener.getPlayerService().getProfile(player.getUUID());

                            if (profile.isEmpty()) {
                                context.getSource().sendFailure(
                                        Component.literal("§cSeu perfil ainda não foi carregado.")
                                );
                                return 0;
                            }

                            EconomyService economyService = new EconomyService();

                            context.getSource().sendSuccess(
                                    () -> Component.literal("§aSeu saldo: §f" + economyService.getBalance(profile.get())),
                                    false
                            );

                            return 1;
                        })
        );
    }
}