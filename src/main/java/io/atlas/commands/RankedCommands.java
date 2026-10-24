package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.ranked.RankedModule;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
public final class RankedCommands {
 private static boolean authenticated(CommandSourceStack s){return !s.isPlayer()||AuthModule.getAuthService().isAuthenticated(s.getPlayer().getUUID());}
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
  var root=Commands.literal("ranked").requires(RankedCommands::authenticated).executes(c->{c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§bRanked: §f/ranked desafiar <jogador> <tier> • /ranked aceitar <jogador> • /ranked recusar <jogador> • /ranked cancelar • /ranking <tier> • /ranked perfil <tier>"),false);return 1;});
  root.then(Commands.literal("desafiar").requires(CommandSourceStack::isPlayer).then(Commands.argument("jogador",EntityArgument.player()).then(Commands.argument("tier",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(new String[]{"OU","UU","MONOTYPE","UBERS","AG"},b)).executes(c->RankedModule.service().challenge(c.getSource(),EntityArgument.getPlayer(c,"jogador"),StringArgumentType.getString(c,"tier"))))));
  for(String action:new String[]{"aceitar","recusar"})root.then(Commands.literal(action).requires(CommandSourceStack::isPlayer).then(Commands.argument("jogador",EntityArgument.player()).executes(c->action.equals("aceitar")?RankedModule.service().accept(c.getSource(),EntityArgument.getPlayer(c,"jogador")):RankedModule.service().decline(c.getSource(),EntityArgument.getPlayer(c,"jogador")))));
  root.then(Commands.literal("cancelar").requires(CommandSourceStack::isPlayer).executes(c->RankedModule.service().cancel(c.getSource())));
  for(String action:new String[]{"ranking","perfil"})root.then(Commands.literal(action).then(Commands.argument("tier",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(new String[]{"OU","UU","MONOTYPE","UBERS","AG"},b)).executes(c->RankedModule.service().query(c.getSource(),StringArgumentType.getString(c,"tier"),action.equals("perfil")))));
  dispatcher.register(root);
  dispatcher.register(Commands.literal("ranking").requires(RankedCommands::authenticated).then(Commands.argument("tier",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(new String[]{"OU","UU","MONOTYPE","UBERS","AG"},b)).executes(c->RankedModule.service().query(c.getSource(),StringArgumentType.getString(c,"tier"),false))));
 }
}
