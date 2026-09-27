package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.home.HomeModule;
import io.atlas.modules.home.menu.HomesMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class HomesCommand {
    private HomesCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("homes")
                .requires(CommandSourceStack::isPlayer)
                .executes(context -> {
                    HomesMenu.open(context.getSource().getPlayerOrException(), HomeModule.getHomeService());
                    return 1;
                }));
    }
}
