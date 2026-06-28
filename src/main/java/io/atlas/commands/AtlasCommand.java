package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class AtlasCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("atlas")
                        .executes(context -> {
                            context.getSource().sendSuccess(
                                    () -> Component.literal("§6Atlas Core §av1.0.0 carregado com sucesso!"),
                                    false
                            );

                            return 1;
                        })
        );
    }
}