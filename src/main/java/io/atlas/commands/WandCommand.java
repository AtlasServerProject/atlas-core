package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.atlas.modules.rank.RankModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class WandCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("wand")
                        .executes(context -> giveWand(context.getSource()))
        );
    }

    private static int giveWand(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!RankModule.getRankService().canManageRanks(player.getUUID())) {
            source.sendFailure(Component.literal("§cWorldEdit é restrito a Dono e ADM."));
            return 0;
        }

        ItemStack wand = new ItemStack(Items.WOODEN_AXE);
        wand.set(DataComponents.CUSTOM_NAME, Component.literal("§6WorldEdit Wand"));

        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        source.sendSuccess(
                () -> Component.literal("§aMachado do WorldEdit entregue. §7Use os cliques para selecionar posições."),
                false
        );
        return 1;
    }
}
