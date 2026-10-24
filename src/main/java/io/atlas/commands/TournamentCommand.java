package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rank.RankModule;
import io.atlas.modules.tournament.TournamentModule;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import io.atlas.modules.tournament.service.TournamentBattleService;
public final class TournamentCommand {
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
  var root=Commands.literal("torneio").requires(s->!s.isPlayer()||AuthModule.getAuthService().isAuthenticated(s.getPlayer().getUUID()))
   .executes(c->TournamentModule.service().command(c.getSource(),"status"));
  for(String action:new String[]{"entrar","sair"})root.then(Commands.literal(action).requires(CommandSourceStack::isPlayer).executes(c->TournamentModule.service().command(c.getSource(),action)));
  for(String action:new String[]{"cancelar","encerrar"})root.then(Commands.literal(action).requires(TournamentCommand::staff).executes(c->TournamentModule.service().command(c.getSource(),action)));
  root.then(Commands.literal("batalha").requires(TournamentCommand::staff)
   .then(Commands.argument("jogador1",EntityArgument.player())
    .then(Commands.argument("jogador2",EntityArgument.player())
     .executes(c->TournamentModule.service().battle(c.getSource(),EntityArgument.getPlayer(c,"jogador1"),EntityArgument.getPlayer(c,"jogador2"),null))
     .then(Commands.argument("tier",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(new String[]{"OU","UU","MONOTYPE","UBERS","AG"},b))
      .executes(c->TournamentModule.service().battle(c.getSource(),EntityArgument.getPlayer(c,"jogador1"),EntityArgument.getPlayer(c,"jogador2"),StringArgumentType.getString(c,"tier")))))));

  root.then(Commands.literal("iniciar").requires(TournamentCommand::staff)
   .then(Commands.argument("tier",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(new String[]{"OU","UU","MONOTYPE","UBERS","AG"},b))
    .then(Commands.argument("jogadores",IntegerArgumentType.integer(8,16)).suggests((c,b)->SharedSuggestionProvider.suggest(new String[]{"8","16"},b))
     .then(Commands.argument("tempo",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(new String[]{"5m","15m","30m","1h"},b))
      .executes(c->TournamentModule.service().startTournament(c.getSource(),StringArgumentType.getString(c,"tier"),IntegerArgumentType.getInteger(c,"jogadores"),StringArgumentType.getString(c,"tempo")))))));
  for(String action:new String[]{"status","chaves"})root.then(Commands.literal(action).executes(c->TournamentModule.service().command(c.getSource(),action)));
  dispatcher.register(root);
 }
 private static boolean staff(CommandSourceStack s){return !s.isPlayer()||RankModule.getRankService().isStaff(s.getPlayer().getUUID());}
}
