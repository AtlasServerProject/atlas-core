package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.moderation.ModerationModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

public final class StaffToolsCommand {
    private StaffToolsCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var service = ModerationModule.getStaffTools();
        dispatcher.register(Commands.literal("freeze").requires(service::canUse)
                .then(Commands.argument("jogador", EntityArgument.player())
                        .executes(ctx -> service.freeze(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador")))));
        for (String name : new String[]{"invsee", "endersee"}) {
            dispatcher.register(Commands.literal(name).requires(source -> source.isPlayer() && service.canUse(source))
                    .then(Commands.argument("jogador", EntityArgument.player())
                            .executes(ctx -> service.inspect(ctx.getSource(), EntityArgument.getPlayer(ctx, "jogador"), name.equals("endersee")))));
        }
    }
}
