package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import io.atlas.modules.performance.PerformanceModule;
import io.atlas.modules.rank.RankModule;
import net.fabricmc.loader.api.FabricLoader;

public class AtlasCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("atlas")
                        .executes(context -> {
                            String version = FabricLoader.getInstance().getModContainer("atlas-core")
                                    .map(container -> container.getMetadata().getVersion().getFriendlyString())
                                    .orElse("desconhecida");
                            context.getSource().sendSuccess(
                                    () -> Component.literal("§6Atlas Core §av" + version + " carregado com sucesso!"),
                                    false
                            );
                            return 1;
                        })
                        .then(Commands.literal("tps")
                                .executes(context -> showPerformance(context.getSource())))
                        .then(Commands.literal("cleanup")
                                .requires(AtlasCommand::canCleanup)
                                .executes(context -> cleanup(context.getSource())))
        );
    }

    private static boolean canCleanup(CommandSourceStack source) {
        var player = source.getPlayer();
        return player == null || RankModule.getRankService().canManageRanks(player.getUUID());
    }

    private static int cleanup(CommandSourceStack source) {
        var result = PerformanceModule.getCleanupService().cleanup(source.getServer());
        source.sendSuccess(() -> Component.literal("§aLimpeza concluída: §f" + result.items()
                + " drops§a, §f" + result.pokemon() + " Pokémon selvagens§a, §7"
                + result.scanned() + " entidades verificadas."), false);
        return result.total();
    }

    private static int showPerformance(CommandSourceStack source) {
        var monitor = PerformanceModule.getTpsMonitor();
        var counts = PerformanceModule.getCleanupService().counts(source.getServer());
        String color = monitor.tps() >= 18.0 ? "§a" : monitor.tps() >= 15.0 ? "§e" : "§c";
        source.sendSuccess(() -> Component.literal(String.format(java.util.Locale.ROOT,
                "§6Atlas Performance §8— %s%.2f TPS §7| §f%.2f MSPT §7| §f%d entidades §8(%d drops, %d Pokémon)",
                color, monitor.tps(), monitor.mspt(), counts.total(), counts.items(), counts.pokemon())), false);
        return 1;
    }
}
