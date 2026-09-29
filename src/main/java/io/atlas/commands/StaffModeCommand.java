package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.moderation.ModerationModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

public final class StaffModeCommand {
    private StaffModeCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var service = ModerationModule.getStaffMode();
        for (String root : new String[]{"staffmode", "staff"}) {
            dispatcher.register(Commands.literal(root).requires(service::canUse)
                    .executes(ctx -> service.toggle(ctx.getSource(), null))
                    .then(Commands.literal("on").executes(ctx -> service.toggle(ctx.getSource(), true)))
                    .then(Commands.literal("off").executes(ctx -> service.toggle(ctx.getSource(), false)))
                    .then(Commands.literal("tp").then(Commands.argument("jogador", EntityArgument.player())
                            .executes(ctx -> service.teleport(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador"))))));
        }
    }
}
