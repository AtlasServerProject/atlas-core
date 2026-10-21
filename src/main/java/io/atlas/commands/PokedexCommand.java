package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.*;
import io.atlas.modules.pokedex.PokedexModule;
public final class PokedexCommand {
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
  for(String alias:new String[]{"academia","dex"}){
   var root=Commands.literal(alias).requires(s->s.isPlayer()&&io.atlas.modules.auth.AuthModule.getAuthService().isAuthenticated(s.getPlayer().getUUID())).executes(c->PokedexModule.rewards().open(c.getSource().getPlayerOrException()));
   var rewards=PokedexModule.rewards();
   root.then(Commands.literal("premios").executes(c->rewards.show(c.getSource().getPlayerOrException())));
   root.then(Commands.literal("resgatar").then(Commands.argument("marco",IntegerArgumentType.integer(10,100)).executes(c->rewards.claim(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"marco")))));
   root.then(Commands.literal("mint").then(Commands.argument("marco",IntegerArgumentType.integer(10,100)).then(Commands.argument("nature",StringArgumentType.word()).executes(c->rewards.mint(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"marco"),StringArgumentType.getString(c,"nature"))))));
   root.then(Commands.literal("lendario").then(Commands.argument("especie",StringArgumentType.word()).executes(c->rewards.chooseLegend(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"especie")))));
   root.then(Commands.literal("lendarios").executes(c->rewards.options(c.getSource().getPlayerOrException(),false)));
   root.then(Commands.literal("mints").executes(c->rewards.options(c.getSource().getPlayerOrException(),true)));
   for(String category:new String[]{"regiao","tipo"})root.then(Commands.literal(category).then(Commands.argument("nome",StringArgumentType.word()).executes(c->PokedexModule.service().show(c.getSource().getPlayerOrException(),category+":"+StringArgumentType.getString(c,"nome")))));
   root.then(Commands.literal("hall").executes(c->PokedexModule.service().hall(c.getSource().getPlayerOrException(),0)).then(Commands.argument("pagina",IntegerArgumentType.integer(1,10000)).executes(c->PokedexModule.service().hall(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"pagina")-1))));dispatcher.register(root);
  }
 }
}
