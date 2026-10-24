package io.atlas.modules.tournament.service;
import io.atlas.modules.tournament.repository.TournamentRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.nio.file.*;

public final class TournamentService {
 private final TournamentRepository repository=new TournamentRepository();
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"atlas-tournament-db");t.setDaemon(true);return t;});
 private volatile MinecraftServer server;private volatile boolean stopped;
 public void start(MinecraftServer s){server=s;worker.scheduleWithFixedDelay(this::announcements,1,2,TimeUnit.SECONDS);}
 public void stop(){stopped=true;worker.shutdown();}
 private boolean configured(){return Files.isRegularFile(Path.of(System.getProperty("user.home"),".config","atlas","challonge.json"));}
 public int battle(CommandSourceStack source,net.minecraft.server.level.ServerPlayer first,net.minecraft.server.level.ServerPlayer second,String requestedTier){
  worker.execute(()->{try{
   String tier=requestedTier;var event=repository.active();
   if(event.isPresent()&&event.get().state().equals("STARTED")){
    String official=event.get().format().substring("atlasgen9".length());
    if(tier!=null&&!TournamentOptions.tier(tier).equals(TournamentOptions.tier(official))){source.getServer().execute(()->source.sendFailure(Component.literal("A tier deve corresponder ao torneio ativo.")));return;}tier=official;
   }else if(tier==null){source.getServer().execute(()->source.sendFailure(Component.literal("Sem torneio iniciado: informe a tier em /torneio batalha <jogador1> <jogador2> <tier>.")));return;}
   String selected=TournamentOptions.tier(tier);source.getServer().execute(()->new TournamentBattleService().start(source,first,second,selected));
  }catch(Exception e){source.getServer().execute(()->source.sendFailure(Component.literal("Não foi possível consultar a tier do confronto.")));}});return 1;
 }
 public int startTournament(CommandSourceStack source,String tier,int capacity,String duration){return command(source,"iniciar",tier,capacity,duration);}
 public int command(CommandSourceStack source,String action){return command(source,action,"OU",16,"30m");}
 private int command(CommandSourceStack source,String action,String tier,int capacity,String duration){
  UUID player=source.getPlayer()==null?null:source.getPlayer().getUUID();String name=source.getTextName();
  worker.execute(()->{String result;
   try{repository.expire();result=switch(action){
    case "iniciar"->{if(!configured())yield "Integração Challonge pendente. Configure a chave no terminal com atlas-challonge login.";String normalized=TournamentOptions.tier(tier);TournamentOptions.capacity(capacity);long seconds=TournamentOptions.seconds(duration);repository.create(name,normalized,capacity,seconds);yield "Preparando Torneio "+normalized+" GEN 9, "+capacity+" vagas, prazo "+duration+". O anúncio será publicado quando o Challonge confirmar a criação.";}
    case "entrar"->repository.enter(player,name);
    case "sair"->repository.leave(player);
    case "cancelar"->repository.cancel();
    case "encerrar"->repository.close();
    default->repository.active().map(e->e.title()+" • "+e.count()+"/"+e.capacity()+" • "+label(e.state())+" • Prazo de inscrição: "+java.time.ZonedDateTime.ofInstant(e.deadline(),java.time.ZoneId.of("America/Fortaleza")).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm:ss"))+" (Brasília)"+"\n"+(e.state().equals("CREATING")?"Preparando ligação com Challonge.":e.url())+(e.error()==null?"":"\nSincronização pendente; a equipe pode consultar a integração.")+"\n/torneio entrar • /torneio sair").orElse("Nenhum torneio ativo.");
   };}catch(IllegalArgumentException e){result=e.getMessage();}catch(Exception e){result="Não foi possível concluir. Tente novamente em instantes.";io.atlas.AtlasMod.LOGGER.warn("Falha no serviço de torneios ({})",e.getClass().getSimpleName());}
   String message=result;if(!stopped)source.getServer().execute(()->source.sendSuccess(()->Component.literal("§b[Atlas • Torneios] §f"+message),false));
  });return 1;
 }
 private String label(String state){return switch(state){case "CREATING"->"Preparando inscrições";case "OPEN"->"Inscrições abertas";case "FULL"->"Preparando chaves";case "STARTED"->"Chaves disponíveis";case "REVIEW"->"Aguardando conferência da equipe";default->state;};}
 private void announcements(){if(stopped||server==null)return;try{repository.expire();for(var n:repository.pending()){
  CompletableFuture<Void> sent=new CompletableFuture<>();server.execute(()->{try{server.getPlayerList().broadcastSystemMessage(Component.literal("§b§l[ATLAS • TORNEIOS] §r§f"+n.message()).withStyle(style->style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,"/torneio"))),false);sent.complete(null);}catch(Exception e){sent.completeExceptionally(e);}});sent.get(5,TimeUnit.SECONDS);repository.delivered(n.id());
 }}catch(Exception e){io.atlas.AtlasMod.LOGGER.warn("Anúncios de torneio pendentes; nova tentativa automática.");}}
}
