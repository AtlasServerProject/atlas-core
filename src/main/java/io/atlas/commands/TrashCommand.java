package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.performance.PerformanceModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class TrashCommand {
    private TrashCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lixeira")
                .executes(context -> {
                    PerformanceModule.getRecoveryService().openTrash(context.getSource().getPlayerOrException());
                    return 1;
                }));
    }
}
