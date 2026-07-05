package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.performance.PerformanceModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class DroppedItemsCommand {
    private DroppedItemsCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dropados")
                .executes(context -> PerformanceModule.getRecoveryService()
                        .openRecovery(context.getSource().getPlayerOrException()) ? 1 : 0));
    }
}
