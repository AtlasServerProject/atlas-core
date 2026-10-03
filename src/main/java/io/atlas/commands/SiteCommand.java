package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.*;
import io.atlas.modules.site.SiteModule;
public final class SiteCommand {
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){dispatcher.register(Commands.literal("site").requires(CommandSourceStack::isPlayer).then(Commands.literal("vincular").then(Commands.argument("codigo",StringArgumentType.word()).executes(c->SiteModule.service().link(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"codigo"))))));}
}
