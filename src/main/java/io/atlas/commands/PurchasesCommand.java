package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.purchases.service.PurchasesService;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
public final class PurchasesCommand {
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){dispatcher.register(Commands.literal("compras").requires(CommandSourceStack::isPlayer).executes(context->{var player=context.getSource().getPlayerOrException();if(!AuthModule.getAuthService().isAuthenticated(player.getUUID())){context.getSource().sendFailure(Component.literal("§cFaça login antes de consultar suas compras."));return 0;}io.atlas.modules.crates.CrateModule.service().openKeys(player);return 1;}));}
}
