package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.lobby.service.DevLobbyAccessService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class DevWorldCommand {

    private static final DevLobbyAccessService service = new DevLobbyAccessService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mundodev")
                .requires(source -> source.isPlayer()
                        && service.canAccess(source.getPlayer()))
                .executes(context -> service.enter(context.getSource().getPlayerOrException())));
    }
}
