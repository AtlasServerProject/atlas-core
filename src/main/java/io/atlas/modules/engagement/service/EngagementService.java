package io.atlas.modules.engagement.service;

import io.atlas.AtlasMod;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rules.RulesModule;
import io.atlas.modules.engagement.model.EngagementPlan;
import io.atlas.modules.engagement.repository.EngagementRepository;
import io.atlas.modules.engagement.repository.EngagementRepository.Reward;
import io.atlas.modules.pokedex.PokedexModule;
import io.atlas.modules.pokedex.repository.DexRewardRepository;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

public final class EngagementService {
    private final EngagementRepository repository=new EngagementRepository();
    private final io.atlas.modules.research.service.ResearchMoneyService money=new io.atlas.modules.research.service.ResearchMoneyService();
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"atlas-professions-daily");t.setDaemon(true);return t;});
    private final Set<UUID> busy=ConcurrentHashMap.newKeySet();
    private final Map<UUID,LocalDate> notified=new HashMap<>();
    private record Snapshot(List<EngagementRepository.Progress> progress,long until){}
    private final Map<UUID,Snapshot> cache=new ConcurrentHashMap<>();
    private MinecraftServer server;
    private int ticks;
    public LocalDate today(){return LocalDate.now(EngagementPlan.ZONE);}
    public void start(MinecraftServer server){this.server=server;validateItems();worker.scheduleWithFixedDelay(this::poll,5,5,TimeUnit.SECONDS);}
    private void validateItems(){for(int d=1;d<=30;d++){var p=EngagementPlan.prize(d);if(p.kind().equals("ITEM")&&net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(p.item()))==net.minecraft.world.item.Items.AIR)throw new IllegalStateException("Daily item unavailable: "+p.item());}}
    private boolean allowed(ServerPlayer p){return p!=null&&!p.hasDisconnected()&&AuthModule.getAuthService().isAuthenticated(p.getUUID())&&!RulesModule.service().pending(p.getUUID());}
    private boolean activityAllowed(ServerPlayer p){return allowed(p)&&!p.isCreative()&&!p.isSpectator()&&Set.of("atlas:survival_emerald","minecraft:the_nether","minecraft:the_end").contains(p.serverLevel().dimension().location().toString());}
    private ServerPlayer player(UUID id){var p=server.getPlayerList().getPlayer(id);return allowed(p)?p:null;}
    private <T>T main(Supplier<T> action)throws Exception{var f=new CompletableFuture<T>();server.execute(()->{try{f.complete(action.get());}catch(Throwable e){f.completeExceptionally(e);}});return f.get(20,TimeUnit.SECONDS);}
    private void message(UUID id,String text){server.execute(()->{var p=player(id);if(p!=null)p.sendSystemMessage(Component.literal(text));});}
    private void work(Runnable action){if(server!=null&&!worker.isShutdown())worker.execute(()->{try{action.run();}catch(Exception e){AtlasMod.LOGGER.warn("Profissões/login diário: operação preservada ou não aplicada",e);}});}
    private void activity(ServerPlayer p,String job,String key,int xp,String world,Long pos){if(!activityAllowed(p))return;var id=p.getUUID();var date=today();work(()->{if(repository.activity(id,job,key,xp,date,world,pos))cache.remove(id);});}
    public void placed(ServerPlayer p,ServerLevel level,BlockPos pos,BlockState state){if(!Set.of("atlas:survival_emerald","minecraft:the_nether","minecraft:the_end").contains(level.dimension().location().toString()))return;
        if(state.is(BlockTags.LOGS)||ore(state)){String world=level.dimension().location().toString();long packed=pos.asLong();work(()->repository.placed(world,packed));}}
    private boolean ore(BlockState state){String name=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();return name.endsWith("_ore")||state.is(Blocks.ANCIENT_DEBRIS);}
    public void broken(net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player who,BlockPos pos,BlockState state){if(!(who instanceof ServerPlayer p)||!activityAllowed(p))return;
        String world=level.dimension().location().toString();String blockKey="natural:"+world+":"+pos.asLong();
        if(ore(state))activity(p,"miner",blockKey,12,world,pos.asLong());
        else if(state.is(BlockTags.LOGS))activity(p,"logger",blockKey,8,world,pos.asLong());
        else if(state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state))activity(p,"farmer","crop:"+world+":"+pos.asLong()+":"+System.currentTimeMillis()/60000,6,null,null);
    }
    public void pokemonFished(com.cobblemon.mod.common.api.events.fishing.BobberSpawnPokemonEvent.Post e){if(e.getBobber().getPlayerOwner() instanceof ServerPlayer p)activity(p,"fisher","fished:"+e.getPokemon().getPokemon().getUuid(),20,null,null);}
    public void piston(net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.core.Direction direction,boolean extend){if(!(level instanceof ServerLevel world))return;var resolver=new net.minecraft.world.level.block.piston.PistonStructureResolver(level,pos,direction,extend);if(!resolver.resolve())return;var moved=extend?direction:direction.getOpposite();for(var from:resolver.getToPush()){var state=level.getBlockState(from);if(state.is(BlockTags.LOGS)||ore(state)){String dimension=world.dimension().location().toString();long before=from.asLong(),after=from.relative(moved).asLong();work(()->{repository.placed(dimension,before);repository.placed(dimension,after);});}}}
    public void fished(ServerPlayer p){activity(p,"fisher","fish:"+UUID.randomUUID(),20,null,null);}
    public void captured(com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent e){activity(e.getPlayer(),"trainer","capture:"+e.getPokemon().getUuid(),35,null,null);}
    public void victory(com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent e){if(e.getWasWildCapture()||e.getLosers().stream().anyMatch(a->a instanceof com.cobblemon.mod.common.battles.actor.PlayerBattleActor))return;
        for(var a:e.getWinners())if(a instanceof com.cobblemon.mod.common.battles.actor.PlayerBattleActor actor){var p=actor.getEntity();if(p!=null)activity(p,"trainer","battle:"+e.getBattle().getBattleId()+":"+p.getUUID(),25,null,null);}}
    public int open(ServerPlayer p,boolean daily){if(!allowed(p)||!busy.add(p.getUUID()))return 0;var id=p.getUUID();work(()->{try{
        var day=repository.daily(id);var snap=cache.get(id);var progress=snap!=null&&snap.until()>System.currentTimeMillis()?snap.progress():repository.progress(id);cache.put(id,new Snapshot(progress,System.currentTimeMillis()+5000));var date=today();
        server.execute(()->{var current=player(id);if(current!=null)current.openMenu(new net.minecraft.world.SimpleMenuProvider((menu,inventory,viewer)->new io.atlas.modules.engagement.menu.EngagementMenu(menu,inventory,daily,day,progress,date),Component.literal(daily?"Atlas • Login diário":"Atlas • Profissões")));});
    }catch(Exception e){message(id,"§cSistema temporariamente indisponível. Tente novamente.");throw e;}finally{busy.remove(id);}});return 1;}
    public void claim(ServerPlayer p,boolean daily){if(!allowed(p)||!busy.add(p.getUUID()))return;var id=p.getUUID();p.closeContainer();var date=today();work(()->{try{
        if(daily){int day=repository.claimDaily(id,date);message(id,day==0?"§eVocê já resgatou hoje. Volte após meia-noite de Brasília.":"§aDia "+day+" resgatado! Entrega em processamento. Os prêmios já ganhos são seus.");}
        else{long amount=repository.claimJobs(id);cache.remove(id);message(id,amount==0?"§eAlcance novos níveis para receber recompensas.":"§aResgate solicitado: "+amount+" CobbleDollars. Entrega em processamento.");}
    }catch(Exception e){message(id,"§cNão foi possível resgatar agora. Tente novamente.");throw e;}finally{busy.remove(id);}});}
    public void tick(MinecraftServer server){if(++ticks%200!=0)return;var date=today();for(var p:server.getPlayerList().getPlayers())if(allowed(p)&&!date.equals(notified.get(p.getUUID()))){notified.put(p.getUUID(),date);var id=p.getUUID();work(()->{try{var d=repository.daily(id);if(EngagementPlan.nextDay(d.last(),d.day(),date)>0)message(id,"§bSeu prêmio diário está disponível! §f/diario §7• 30 dias consecutivos. Se perder um dia, volta ao dia 1.");}catch(Exception e){server.execute(()->notified.remove(id));throw e;}});}}
    private DexRewardRepository.Reward dex(Reward r){return new DexRewardRepository.Reward(r.id(),r.player(),1,0,r.kind(),r.value(),r.amount(),r.state(),r.payload(),"main");}
    private void poll(){try{for(var initial:repository.pending()){try{
        if(!main(()->player(initial.player())!=null))continue;Reward r=initial;
        if(r.state().equals("PENDING")){String payload=r.kind().equals("MONEY")?"{}":main(()->PokedexModule.rewards().generate(dex(initial)));if(payload==null||!repository.prepare(r.id(),payload))continue;r=new Reward(r.id(),r.player(),r.source(),r.key(),r.kind(),r.value(),r.amount(),"READY",payload);}
        final Reward current=r;boolean recovery=r.state().equals("DELIVERING");if(!recovery&&!repository.state(r.id(),"READY","DELIVERING"))continue;
        if(r.kind().equals("MONEY")){
            boolean delivered=main(()->{var p=player(current.player());if(p==null)return false;try{return money.move(p,current.id(),Long.parseLong(current.value()),"atlasJobPaid:",false,recovery);}catch(Exception e){throw new IllegalStateException(e);}});
            if(!delivered){if(!recovery)repository.state(r.id(),"DELIVERING","READY");continue;}
        }else{
            var saved=main(()->PokedexModule.rewards().deliver(dex(current),recovery));if(saved==null){if(!recovery)repository.state(r.id(),"DELIVERING","READY");continue;}for(var barrier:saved.barriers())barrier.get(20,TimeUnit.SECONDS);
        }
        if(repository.state(r.id(),"DELIVERING","DELIVERED"))message(r.player(),"§aRecompensa de "+(r.source().equals("daily")?"login diário":"profissão")+" entregue! Confira seu inventário, equipe/PC ou saldo.");
    }catch(Exception e){Throwable cause=e;while(cause.getCause()!=null)cause=cause.getCause();if(Set.of("UNCERTAIN_PAYMENT","UNCERTAIN_DELIVERY").contains(String.valueOf(cause.getMessage())))repository.state(initial.id(),"DELIVERING","REVIEW");AtlasMod.LOGGER.warn("Recompensa {} aguardando retry/conferência",initial.id(),e);}}}catch(Exception e){AtlasMod.LOGGER.warn("Fila de profissões/diário indisponível",e);}}
    public void remove(UUID id){cache.remove(id);notified.remove(id);}
    public void stop(){worker.shutdown();}
}
