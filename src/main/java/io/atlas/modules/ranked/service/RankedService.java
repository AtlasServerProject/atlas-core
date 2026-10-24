package io.atlas.modules.ranked.service;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.battles.model.BattleEndResult;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import io.atlas.modules.battle.service.PlayerBattleChecks;
import io.atlas.modules.ranked.repository.*;
import io.atlas.modules.tournament.service.TournamentOptions;
import io.atlas.modules.audit.service.ActivityLogService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public final class RankedService {
 private record Challenge(UUID from,UUID to,String tier,long deadline){}
 private static final class Match {
  final UUID id,first,second;final String tier;boolean resolved;
  Match(UUID id,UUID first,UUID second,String tier){this.id=id;this.first=first;this.second=second;this.tier=tier;}
 }
 private record Cached(long at,List<RankedRepository.Standing> rows){}
 private final RankedRepository repo=new RankedRepository();
 private final RankedResultJournal journal=new RankedResultJournal(Path.of("config","atlas-ranked","pending"));
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"atlas-ranked-db");t.setDaemon(true);return t;});
 private final Map<UUID,Challenge> challenges=new HashMap<>();
 private final Set<UUID> busy=new HashSet<>();
 private final Map<UUID,Match> live=new HashMap<>();
 private final Map<UUID,Long> ended=new HashMap<>();
 private final Map<UUID,RankedResultJournal.Result> unsaved=new ConcurrentHashMap<>();
 private final Map<UUID,String> cancellations=new ConcurrentHashMap<>();
 private final Map<String,Cached> cache=new ConcurrentHashMap<>();
 private MinecraftServer server;private volatile boolean ready,stopped;private long lastTick;
 public void start(MinecraftServer s){server=s;worker.scheduleWithFixedDelay(this::flush,0,5,TimeUnit.SECONDS);}
 public void stop(){stopped=true;ready=false;worker.shutdown();try{worker.awaitTermination(3,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
 private void tell(UUID id,String message){var p=server.getPlayerList().getPlayer(id);if(p!=null)p.sendSystemMessage(Component.literal("§b[Ranked] §f"+message));}
 private void both(UUID a,UUID b,String text){tell(a,text);tell(b,text);}
 private int fail(CommandSourceStack source,String text){source.sendFailure(Component.literal(text));return 0;}
 private void main(Runnable action){if(!stopped)server.execute(()->{if(!stopped)action.run();});}
 public int challenge(CommandSourceStack source,ServerPlayer target,String input)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
  var from=source.getPlayerOrException();String tier;
  try{tier=TournamentOptions.tier(input);}catch(IllegalArgumentException e){return fail(source,e.getMessage());}
  if(!ready)return fail(source,"O ranking está conectando ao banco. Tente novamente em instantes.");
  tick(server);
  String error=PlayerBattleChecks.error(server,from,target);if(error!=null)return fail(source,error);
  if(busy.contains(from.getUUID())||busy.contains(target.getUUID()))return fail(source,"Um dos jogadores está preparando ou encerrando uma ranqueada.");
  if(challenges.values().stream().anyMatch(c->c.from.equals(from.getUUID())||c.to.equals(from.getUUID())||c.from.equals(target.getUUID())||c.to.equals(target.getUUID())))return fail(source,"Um dos jogadores já possui um convite pendente. Use /ranked cancelar ou aguarde.");
  challenges.put(from.getUUID(),new Challenge(from.getUUID(),target.getUUID(),tier,System.nanoTime()+TimeUnit.SECONDS.toNanos(60)));
  tell(from.getUUID(),"Desafio "+tier+" enviado a "+target.getName().getString()+". Validade: 60 segundos.");
  tell(target.getUUID(),from.getName().getString()+" desafiou você na tier "+tier+"! Vale pontos de ranking. Confira o regulamento antes de aceitar.");
  target.sendSystemMessage(Component.literal("§a[ACEITAR] ").withStyle(s->s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ranked aceitar "+from.getName().getString())))
   .append(Component.literal("§c[RECUSAR]").withStyle(s->s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/ranked recusar "+from.getName().getString())))));
  return 1;
 }
 public int accept(CommandSourceStack source,ServerPlayer challenger)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
  var target=source.getPlayerOrException();var challenge=challenges.get(challenger.getUUID());
  if(challenge==null||!challenge.to.equals(target.getUUID())||challenge.deadline<=System.nanoTime())return fail(source,"Não há convite válido desse jogador para você.");
  if(!ready)return fail(source,"O ranking está indisponível. Tente novamente.");
  String error=PlayerBattleChecks.error(server,challenger,target);if(error!=null)return fail(source,error);
  if(busy.contains(challenger.getUUID())||busy.contains(target.getUUID()))return fail(source,"Um jogador já está em preparação ranqueada.");
  challenges.remove(challenger.getUUID());UUID a=challenger.getUUID(),b=target.getUUID(),id=UUID.randomUUID();String an=challenger.getName().getString(),bn=target.getName().getString();
  busy.add(a);busy.add(b);both(a,b,"Desafio aceito. Preparando a batalha "+challenge.tier+"...");
  worker.execute(()->{try{repo.prepare(id,challenge.tier,a,an,b,bn);main(()->begin(new Match(id,a,b,challenge.tier)));}
   catch(Exception e){main(()->{busy.remove(a);busy.remove(b);both(a,b,"Não foi possível reservar a partida. Nenhum ponto foi alterado.");});io.atlas.AtlasMod.LOGGER.warn("Reserva de ranqueada falhou ({})",e.getClass().getSimpleName());}});
  return 1;
 }
 private void cancelMatch(Match match,String reason){busy.remove(match.first);busy.remove(match.second);both(match.first,match.second,reason+" Nenhum ponto foi alterado.");cancellations.put(match.id,reason);worker.execute(this::flush);}
 private void begin(Match match){
  var a=server.getPlayerList().getPlayer(match.first);var b=server.getPlayerList().getPlayer(match.second);
  if(a==null||b==null){cancelMatch(match,"Um jogador desconectou antes do início.");return;}
  String error=PlayerBattleChecks.error(server,a,b);if(error!=null){cancelMatch(match,error);return;}
  io.atlas.modules.competitive.CompetitiveModule.service().begin(match.tier,a,b,success->{
    var battle=success.getBattle();UUID battleId=battle.getBattleId();live.put(battleId,match);
    battle.getOnEndHandlers().add(endedBattle->{ended.put(battleId,System.nanoTime()+TimeUnit.SECONDS.toNanos(2));return kotlin.Unit.INSTANCE;});
    worker.execute(()->{try{repo.activate(match.id,battleId);}catch(Exception e){io.atlas.AtlasMod.LOGGER.error("Vínculo de batalha ranqueada pendente; resultado usará reserva persistente",e);}});
    both(match.first,match.second,"Ranqueada "+match.tier+" iniciada com cláusulas ativas! Boa batalha.");
    ActivityLogService.record("RANKED_START",a.getName().getString(),a.getUUID().toString(),"ranked",match.tier+": "+a.getName().getString()+" x "+b.getName().getString());
   },reason->cancelMatch(match,reason));
 }

 public void victory(BattleVictoryEvent event){
  var match=live.get(event.getBattle().getBattleId());if(match==null||match.resolved||event.getWasWildCapture())return;
  if(!io.atlas.modules.competitive.CompetitiveModule.service().resultAllowed(event.getBattle()))return;
  var result=event.getBattle().getEndResult();if(result==BattleEndResult.STOPPED||result==BattleEndResult.CAPTURE||result==BattleEndResult.FLED)return;
  var winners=event.getWinners().stream().filter(a->a instanceof PlayerBattleActor).map(a->a.getUuid()).toList();
  var losers=event.getLosers().stream().filter(a->a instanceof PlayerBattleActor).map(a->a.getUuid()).toList();
  if(winners.size()!=1||losers.size()!=1)return;UUID winner=winners.getFirst(),loser=losers.getFirst();
  if(!(winner.equals(match.first)&&loser.equals(match.second)||winner.equals(match.second)&&loser.equals(match.first)))return;
  match.resolved=true;var outcome=new RankedResultJournal.Result(match.id,event.getBattle().getBattleId(),winner,loser);unsaved.put(match.id,outcome);
  try{journal.save(outcome);unsaved.remove(match.id);}catch(Exception e){io.atlas.AtlasMod.LOGGER.error("Resultado ranqueado aguardando gravação durável",e);}
  worker.execute(this::flush);
 }
 private void flush(){
  try{
   for(var entry:List.copyOf(cancellations.entrySet())){repo.cancel(entry.getKey(),entry.getValue());cancellations.remove(entry.getKey());}
   for(var result:List.copyOf(unsaved.values())){journal.save(result);unsaved.remove(result.match());}
   for(var result:journal.pending()){
    var change=repo.settle(result.match(),result.battle(),result.winner(),result.loser());journal.done(result.match());
    change.ifPresent(c->{cache.remove(c.tier());main(()->{tell(c.winner(),c.tier()+": vitória! +"+c.delta()+" Elo • "+c.winnerRating()+" pontos.");tell(c.loser(),c.tier()+": derrota. -"+c.delta()+" Elo • "+c.loserRating()+" pontos.");});});
   }
   if(!ready&&!stopped){repo.recover();ready=true;}
  }catch(Exception e){io.atlas.AtlasMod.LOGGER.warn("Ranking aguardando persistência; nova tentativa automática ({})",e.getClass().getSimpleName());}
 }
 public void tick(MinecraftServer s){
  if(stopped||server==null)return;long now=System.nanoTime();if(now-lastTick<TimeUnit.SECONDS.toNanos(1))return;lastTick=now;
  challenges.values().removeIf(c->{if(c.deadline<=now||server.getPlayerList().getPlayer(c.from)==null||server.getPlayerList().getPlayer(c.to)==null){both(c.from,c.to,"Convite encerrado: prazo expirado ou jogador desconectado.");return true;}return false;});
  var iterator=ended.entrySet().iterator();while(iterator.hasNext()){var e=iterator.next();if(e.getValue()>now)continue;var match=live.remove(e.getKey());iterator.remove();if(match!=null){busy.remove(match.first);busy.remove(match.second);if(!match.resolved)cancelMatch(match,"Batalha encerrada sem vencedor confirmado.");}}
 }
 public int decline(CommandSourceStack source,ServerPlayer challenger)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
  var player=source.getPlayerOrException();var c=challenges.get(challenger.getUUID());
  if(c==null||!c.to.equals(player.getUUID()))return fail(source,"Não há convite desse jogador para você.");
  challenges.remove(c.from);both(c.from,c.to,"Convite recusado.");return 1;
 }
 public int cancel(CommandSourceStack source)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
  UUID id=source.getPlayerOrException().getUUID();var found=challenges.values().stream().filter(c->c.from.equals(id)||c.to.equals(id)).findFirst();
  if(found.isEmpty())return fail(source,"Você não possui convite pendente.");var c=found.get();challenges.remove(c.from);both(c.from,c.to,"Convite cancelado.");return 1;
 }
 public int query(CommandSourceStack source,String input,boolean personal){
  String tier;try{tier=TournamentOptions.tier(input);}catch(IllegalArgumentException e){return fail(source,e.getMessage());}
  UUID id=source.isPlayer()?source.getPlayer().getUUID():null;if(personal&&id==null)return fail(source,"Use esse comando no jogo.");
  worker.execute(()->{try{
   String message;
   if(personal){message=repo.profile(tier,id).map(r->tier+" • #"+r.position()+" • "+r.rating()+" Elo • "+r.wins()+" vitórias / "+r.losses()+" derrotas").orElse(tier+" • 1.000 Elo inicial • sem partidas ranqueadas concluídas.");}
   else{Cached cached=cache.get(tier);if(cached==null||System.nanoTime()-cached.at>TimeUnit.SECONDS.toNanos(15)){cached=new Cached(System.nanoTime(),repo.top(tier));cache.put(tier,cached);}var lines=new StringBuilder("Ranking "+tier+" • Top 10");for(var r:cached.rows)lines.append("\n#").append(r.position()).append(" ").append(r.name()).append(" • ").append(r.rating()).append(" Elo • ").append(r.wins()).append("V/").append(r.losses()).append("D");if(cached.rows.isEmpty())lines.append("\nNenhuma partida ranqueada concluída.");message=lines.toString();}
   main(()->source.sendSuccess(()->Component.literal("§b[Ranked] §f"+message),false));
  }catch(Exception e){main(()->source.sendFailure(Component.literal("Ranking temporariamente indisponível.")));}});return 1;
 }
}
