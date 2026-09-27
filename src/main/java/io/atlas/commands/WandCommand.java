package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.atlas.modules.rank.RankModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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

        // O WorldEdit identifica a ferramenta pelo item configurado no
        // worldedit.properties (minecraft:wooden_axe). Entregamos a stack
        // vanilla sem nome/NBT customizado para manter o reconhecimento
        // idêntico ao //wand nativo do WorldEdit.
        ItemStack wand = new ItemStack(Items.WOODEN_AXE);

        player.setItemInHand(InteractionHand.MAIN_HAND, wand);
        source.sendSuccess(
                () -> Component.literal("§aMachado do WorldEdit entregue. §7Use //pos1 e //pos2 para selecionar."),
                false
        );
        return 1;
    }
}
