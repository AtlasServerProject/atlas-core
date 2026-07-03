package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.home.HomeModule;
import io.atlas.modules.home.model.Home;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class HomesCommand {

    private HomesCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("homes")
                .requires(CommandSourceStack::isPlayer)
                .executes(context -> execute(context.getSource())));
    }

    private static int execute(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        var service = HomeModule.getHomeService();
        List<Home> homes = service.listHomes(player.getUUID());
        if (homes.isEmpty()) {
            source.sendFailure(Component.literal("§eVocê ainda não possui homes."));
            return 0;
        }
        String names = homes.stream()
                .map(home -> (home.primary() ? "§a★ " : "§7") + home.name())
                .reduce((left, right) -> left + "§8, " + right)
                .orElse("");
        source.sendSuccess(() -> Component.literal(
                "§6Homes §7(" + homes.size() + "/" + service.homeLimit(player.getUUID()) + "): " + names
        ), false);
        return homes.size();
    }
}
