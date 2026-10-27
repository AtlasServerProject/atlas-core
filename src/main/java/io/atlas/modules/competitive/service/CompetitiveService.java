package io.atlas.modules.competitive.service;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.*;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.pokemon.helditem.HeldItemProvider;
import io.atlas.modules.battle.service.PlayerBattleChecks;
import io.atlas.modules.competitive.repository.CompetitiveValidator;
import io.atlas.modules.audit.service.ActivityLogService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class CompetitiveService {
 private record Snapshot(String packed,String stable,List<Pokemon> originals){}
 private static final class Guard {
  final PokemonBattle battle;final UUID a,b;final String tier,sa,sb;final List<Pokemon> originals;boolean invalid;long endedAt;
  Guard(PokemonBattle battle,UUID a,UUID b,String tier,Snapshot first,Snapshot second){this.battle=battle;this.a=a;this.b=b;this.tier=tier;sa=first.stable;sb=second.stable;originals=new ArrayList<>(first.originals);originals.addAll(second.originals);}
 }
 private final CompetitiveValidator validator=new CompetitiveValidator();
 private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"atlas-competitive-validator");t.setDaemon(true);return t;});
 private final Set<UUID> preparing=new HashSet<>();private final Map<UUID,Guard> guards=new HashMap<>();
 private MinecraftServer server;private boolean stopped;private long lastTick;
 public void start(MinecraftServer s){server=s;}
 public void stop(){stopped=true;worker.shutdownNow();}
 private Snapshot snapshot(ServerPlayer p){
  var party=Cobblemon.INSTANCE.getStorage().getParty(p);var team=party.toBattleTeam(false,false,null);
  for(var member:team)if(!member.getOriginalPokemon().heldItem().isEmpty()&&HeldItemProvider.provideShowdownId(member).isBlank())throw new IllegalArgumentException("Item segurado sem identificação competitiva: remova-o para batalhar.");
  String packed=BattleRegistry.INSTANCE.packTeam(team);var originals=new ArrayList<Pokemon>();var order=new StringBuilder();for(var mon:party)if(mon!=null){originals.add(mon);order.append(mon.getUuid()).append(',');}
  return new Snapshot(packed,order+CompetitivePolicy.stable(packed),originals);
 }
 private void tell(ServerPlayer p,String text){if(p!=null)p.sendSystemMessage(Component.literal("§b[Competitivo] §f"+text));}
 public void begin(String input,ServerPlayer a,ServerPlayer b,Consumer<SuccessfulBattleStart> success,Consumer<String> failure){
  String tier=io.atlas.modules.tournament.service.TournamentOptions.tier(input);String error=PlayerBattleChecks.error(server,a,b);
  if(error!=null){failure.accept(error);return;}
  if(preparing.contains(a.getUUID())||preparing.contains(b.getUUID())){failure.accept("Um jogador já está em validação competitiva.");return;}
  Snapshot first,second;try{first=snapshot(a);second=snapshot(b);}catch(RuntimeException e){failure.accept(e.getMessage());return;}
  preparing.add(a.getUUID());preparing.add(b.getUUID());tell(a,"Validando equipe para "+tier+"...");tell(b,"Validando equipe para "+tier+"...");
  worker.execute(()->{
   com.google.gson.JsonObject report;try{report=validator.validate(tier,first.packed,second.packed);}catch(Exception e){io.atlas.AtlasMod.LOGGER.warn("Validação competitiva indisponível ({})",e.getClass().getSimpleName());report=null;}
   var response=report;if(stopped)return;server.execute(()->{
    preparing.remove(a.getUUID());preparing.remove(b.getUUID());if(stopped)return;
    if(response==null){failure.accept("Validação indisponível. A batalha não foi iniciada; tente novamente mais tarde.");return;}
    if(!response.get("ok").getAsBoolean()){
     var reports=response.getAsJsonArray("reports");for(int i=0;i<2;i++)for(var problem:reports.get(i).getAsJsonArray())tell(i==0?a:b,"Equipe inválida: "+problem.getAsString());
     failure.accept("Uma equipe não atende às regras de "+tier+". Os motivos foram enviados ao respectivo jogador.");return;
    }
    String unavailable=PlayerBattleChecks.error(server,a,b);if(unavailable!=null){failure.accept(unavailable);return;}
    try{
     if(!first.stable.equals(snapshot(a).stable)||!second.stable.equals(snapshot(b).stable)){failure.accept("Uma equipe mudou durante a validação. Tente novamente.");return;}
     var rules=new LinkedHashSet<>(List.of(CompetitivePolicy.format(tier),"Dynamax Clause","Bag Clause","!Team Preview"));
     var format=new BattleFormat("cobblemon",BattleTypes.INSTANCE.getSINGLES(),rules,9,-1);
     // Clone teams: battle damage, transformations and consumed items do not mutate the validated source team.
     var result=BattleBuilder.INSTANCE.pvp1v1(a,b,null,null,format,true,false);
     if(result instanceof SuccessfulBattleStart started){
      var battle=started.getBattle();try{CompetitiveTeamStoreService.attach(battle,a.getUUID(),b.getUUID());}catch(RuntimeException setupError){battle.stop();throw setupError;}var guard=new Guard(battle,a.getUUID(),b.getUUID(),tier,first,second);guards.put(battle.getBattleId(),guard);
      ActivityLogService.record("COMPETITIVE_VALIDATED","Atlas",battle.getBattleId().toString(),"competitive",tier+" • "+CompetitivePolicy.VERSION);
      battle.getOnEndHandlers().add(ended->{guard.endedAt=System.nanoTime();return kotlin.Unit.INSTANCE;});success.accept(started);
     }else{
      if(result instanceof ErroredBattleStart rejected){rejected.sendTo(a,m->m);rejected.sendTo(b,m->m);}
      failure.accept("O motor recusou o confronto competitivo.");
     }
    }catch(RuntimeException e){io.atlas.AtlasMod.LOGGER.error("Início competitivo falhou",e);failure.accept("Não foi possível iniciar a batalha competitiva.");}
   });
  });
 }
 public boolean originalLocked(Pokemon pokemon){return guards.values().stream().anyMatch(g->!g.battle.getEnded()&&g.originals.stream().anyMatch(p->p==pokemon));}
 private boolean intact(Guard g){
  var format=g.battle.getFormat();
  if(!format.getMod().equals("cobblemon")||format.getGen()!=9||!format.getRuleSet().containsAll(List.of(CompetitivePolicy.format(g.tier),"Dynamax Clause","Bag Clause","!Team Preview")))return false;
  var a=server.getPlayerList().getPlayer(g.a);var b=server.getPlayerList().getPlayer(g.b);
  // A disconnect can produce a legitimate native forfeit; keep winner handling intact.
  try{return (a==null||g.sa.equals(snapshot(a).stable))&&(b==null||g.sb.equals(snapshot(b).stable));}catch(Exception e){return false;}
 }
 public boolean resultAllowed(PokemonBattle battle){var g=guards.get(battle.getBattleId());if(g==null)return false;if(!g.invalid&&!intact(g))invalidate(g,"Equipe alterada externamente durante o confronto");return !g.invalid;}
 private void invalidate(Guard g,String reason){
  if(g.invalid)return;g.invalid=true;tell(server.getPlayerList().getPlayer(g.a),"Confronto anulado: "+reason+". Sem pontos de ranking.");tell(server.getPlayerList().getPlayer(g.b),"Confronto anulado: "+reason+". Sem pontos de ranking.");
  ActivityLogService.record("COMPETITIVE_INVALID", "Atlas",g.battle.getBattleId().toString(),"competitive",g.tier+": "+reason);
  if(!g.battle.getEnded())g.battle.stop();
 }
 public void forbidden(PokemonBattle battle,String mechanic){var g=guards.get(battle.getBattleId());if(g!=null)invalidate(g,mechanic+" não permitido na Gen 9 deste formato");}
 public void tera(PokemonBattle battle){var g=guards.get(battle.getBattleId());if(g!=null&&g.tier.equals("MONOTYPE"))invalidate(g,"Terastalização proibida em Monotype");}
 public void tick(MinecraftServer s){
  if(stopped||server==null)return;long now=System.nanoTime();if(now-lastTick<TimeUnit.MILLISECONDS.toNanos(500))return;lastTick=now;
  for(var g:List.copyOf(guards.values())){
   if(g.endedAt>0&&now-g.endedAt>TimeUnit.SECONDS.toNanos(10)){guards.remove(g.battle.getBattleId());continue;}
   if(!g.battle.getEnded()&&!g.invalid&&!intact(g))invalidate(g,"Equipe alterada externamente durante o confronto");
  }
 }
}
