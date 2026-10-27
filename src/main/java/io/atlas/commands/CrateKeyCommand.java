package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.*;import net.minecraft.commands.arguments.EntityArgument;
import io.atlas.modules.crates.CrateModule;import io.atlas.modules.auth.AuthModule;import io.atlas.permission.Permission;
public final class CrateKeyCommand {
 public static void register(CommandDispatcher<CommandSourceStack> d){d.register(Commands.literal("chaves").executes(c->{CrateModule.service().openKeys(c.getSource().getPlayerOrException());return 1;}).then(Commands.literal("dar").requires(s->!s.isPlayer()||(AuthModule.getAuthService().isAuthenticated(s.getPlayer().getUUID())&&Permission.has(s.getPlayer(),"atlas.crates.keys.grant"))).then(Commands.argument("jogador",EntityArgument.player()).then(Commands.argument("caixa",StringArgumentType.word()).suggests((c,b)->{for(int i=1;i<=9;i++)b.suggest("generation_"+i);b.suggest("legendary_monthly");return b.buildFuture();}).then(Commands.argument("quantidade",IntegerArgumentType.integer(1,1000)).executes(c->{var target=EntityArgument.getPlayer(c,"jogador");CrateModule.service().grantKeys(c.getSource(),target.getUUID(),StringArgumentType.getString(c,"caixa"),IntegerArgumentType.getInteger(c,"quantidade"));return 1;}))))));}
}
