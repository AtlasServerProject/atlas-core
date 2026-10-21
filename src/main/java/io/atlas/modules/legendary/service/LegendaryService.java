package io.atlas.modules.legendary.service;

import io.atlas.AtlasMod;
import io.atlas.modules.legendary.config.LegendaryConfig;
import io.atlas.modules.legendary.model.LegendaryRules;
import io.atlas.modules.legendary.repository.LegendaryRepository;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.api.entity.Despawner;
import com.cobblemon.mod.common.api.events.pokemon.PokemonCapturedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class LegendaryService {
    private static final TicketType<UUID> TICKET=TicketType.create("atlas_legendary",Comparator.comparing(UUID::toString));
    private static final Despawner<PokemonEntity> NO_DESPAWN=new Despawner<>(){public void beginTracking(PokemonEntity p){}public boolean shouldDespawn(PokemonEntity p){return false;}};
    private final LegendaryRepository repository=new LegendaryRepository();
    private final LegendaryEligibilityService eligibility=new LegendaryEligibilityService();
    private final LegendaryBossBarService bossBars=new LegendaryBossBarService();
    private final LegendaryAnnouncementService announcements=new LegendaryAnnouncementService();
    private final java.util.random.RandomGenerator random=new java.util.Random();
    private final Map<UUID,Active> active=new HashMap<>();
    private MinecraftServer server;
    private LegendaryConfig config;
    private LegendaryRepository.State state;
    private boolean busy,ready,stopped;
    private long checked;
    private static final class Active {
        final LegendaryRepository.Event event;
        final LegendaryConfig.Spawn rule;
        PokemonEntity entity;
        ChunkPos chunk;
        long recoverUntil,lastMove;
        boolean finishing;
        Active(LegendaryRepository.Event e,LegendaryConfig.Spawn rule){event=e;this.rule=rule;}
    }
    private <T>void complete(CompletableFuture<T> f,Consumer<T> success){f.whenComplete((value,error)->{
        if(server==null||stopped)return;
        server.execute(()->{if(stopped)return;if(error!=null){ready=false;busy=false;AtlasMod.LOGGER.error("Sistema de lendários suspenso por falha de persistência.",error);}else success.accept(value);});
    });}
    public void start(MinecraftServer server){
        this.server=server;
        try{config=LegendaryConfig.load(server);}catch(Exception e){AtlasMod.LOGGER.error("Configuração de lendários rejeitada; nenhum spawn será executado.",e);return;}
        long now=System.currentTimeMillis();
        complete(repository.load(now+config.main.intervalMillis(),config.main.enabled),loaded->{state=loaded;
            if(state.next()<=now&&config.main.offlinePolicy.equals("SKIP")){
                var recovery=LegendaryRules.recover(state.next(),now,config.main.intervalMillis());
                complete(repository.recover(state.next(),recovery.next(),recovery.missed()),s->{state=s;AtlasMod.LOGGER.info("Lendários: {} ciclos perdidos durante indisponibilidade; próxima tentativa futura preservada.",recovery.missed());restore();});
            }else restore();
        });
    }
    private void restore(){complete(repository.active(),events->{
        for(var event:events){
            var rule=rule(event.species());
            if(rule==null){complete(repository.transition(event.id(),"LOST"),ignored->{});continue;}
            Active a=new Active(event,rule);a.recoverUntil=System.currentTimeMillis()+15000;active.put(event.pokemon(),a);
            ServerLevel world=world(event.dimension());
            if(world==null){finish(a,"LOST");continue;}
            a.chunk=new ChunkPos(BlockPos.containing(event.x(),event.y(),event.z()));
            world.getChunkSource().addRegionTicket(TICKET,a.chunk,2,event.id());
            // Load only the saved event chunk, never scan regions or create replacement Pokémon.
            world.getChunk(a.chunk.x,a.chunk.z);
        }
        ready=true;AtlasMod.LOGGER.info("Lendários: {} espécies configuradas, {} indisponíveis; ciclo persistente carregado.",config.spawns.size(),config.unavailable.size());
    });}
    private ServerLevel world(String dimension){return server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(dimension)));}
    private LegendaryConfig.Spawn rule(String species){return config.spawns.stream().filter(e->e.species.equals(species)).findFirst().orElse(null);}
    public void loaded(PokemonEntity entity){
        if(server==null||stopped)return;Active a=active.get(entity.getPokemon().getUuid());
        if(a!=null&&entity.getUUID().equals(a.event.entity())&&entity.getOwner()==null){a.entity=entity;protect(entity);}
        else if(entity.getOwner()==null&&entity.getPokemon().getPersistentData().hasUUID("atlasLegendaryEvent")){
            complete(repository.status(entity.getPokemon().getUuid()),status->{
                if(status!=null&&!status.equals("ACTIVE")&&!status.equals("PENDING")&&!status.equals("CAPTURED")&&entity.getOwner()==null&&entity.getBattleId()==null&&!entity.isBusy())entity.discard();
            });
        }
    }
    private void protect(PokemonEntity entity){entity.setPersistenceRequired();entity.setDespawner(NO_DESPAWN);}
    public void tick(MinecraftServer server){
        if(!ready||stopped)return;long now=System.currentTimeMillis();if(now-checked<1000)return;checked=now;
        maintain(now);
        if(busy||!state.enabled()||!config.main.enabled)return;
        if(now>=state.next()){attempt(null,null,false);return;}
        if(!state.warned()&&now>=state.next()-config.main.warningMinutes*60000L){busy=true;long expected=state.next();
            complete(repository.warn(expected),won->{busy=false;if(won){state=new LegendaryRepository.State(state.last(),state.next(),true,state.enabled(),state.missed());announcements.warning(server,config.main.warningMinutes);}});
        }
    }
    private void maintain(long now){
        for(Active a:List.copyOf(active.values())){
            if(a.finishing)continue;ServerLevel w=world(a.event.dimension());if(w==null){finish(a,"LOST");continue;}
            if(a.entity==null){var found=w.getEntity(a.event.entity());if(found instanceof PokemonEntity p&&p.getPokemon().getUuid().equals(a.event.pokemon())){a.entity=p;protect(p);}}
            PokemonEntity p=a.entity;
            if(p==null){bossBars.remove(a.event.id());if(now>a.recoverUntil)finish(a,"LOST");continue;}
            if(p.getOwner()!=null){/* Only POKEMON_CAPTURED may announce; never infer a capture from ownership. */finish(a,"LOST");continue;}
            if(p.isRemoved()){
                if(p.getRemovalReason()==net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK){a.entity=null;a.recoverUntil=now+15000;continue;}
                finish(a,p.getPokemon().getCurrentHealth()<=0?"DEFEATED":"DESPAWNED");continue;
            }
            ChunkPos current=p.chunkPosition();
            if(!current.equals(a.chunk)){
                if(a.chunk!=null)w.getChunkSource().removeRegionTicket(TICKET,a.chunk,2,a.event.id());
                a.chunk=current;w.getChunkSource().addRegionTicket(TICKET,current,2,a.event.id());
                complete(repository.moved(a.event.id(),p.getX(),p.getY(),p.getZ()),v->{});
            }
            if(now-a.lastMove>=10000){a.lastMove=now;complete(repository.moved(a.event.id(),p.getX(),p.getY(),p.getZ()),v->{});}
            String outcome=LegendaryRules.liveOutcome(p.getPokemon().getCurrentHealth(),p.getBattleId()!=null,p.isBusy(),now,a.event.expires());
            if(outcome!=null){p.discard();finish(a,outcome);}
            else bossBars.update(server,a.event,a.rule.displayName,now,p.getBattleId()!=null||p.isBusy(),Set.copyOf(config.main.allowedWorlds));
        }
    }
    private void release(Active a){bossBars.remove(a.event.id());var w=world(a.event.dimension());if(w!=null&&a.chunk!=null)w.getChunkSource().removeRegionTicket(TICKET,a.chunk,2,a.event.id());active.remove(a.event.pokemon());}
    private void finish(Active a,String status){if(a.finishing)return;a.finishing=true;complete(repository.transition(a.event.id(),status),won->{release(a);if(won){announcements.outcome(server,config,a.rule,status,a.event.test());complete(repository.announced(a.event.id()),v->{});AtlasMod.LOGGER.info("Evento lendário {} encerrado: {}",a.event.id(),status);}});}
    public void captured(PokemonCapturedEvent event){
        if(server==null||stopped)return;
        if(!server.isSameThread()){server.execute(()->captured(event));return;}
        var pokemon=event.getPokemon();Active a=active.get(pokemon.getUuid());
        if(a==null||a.finishing||!pokemon.getSpecies().getResourceIdentifier().getPath().equals(a.event.species()))return;
        a.finishing=true;var data=LegendaryAnnouncementService.captureData(pokemon,event.getPlayer());
        complete(repository.capture(pokemon.getUuid(),event.getPlayer().getUUID(),data.toString()),won->{
            release(a);if(won){announcements.capture(server,config,a.rule,data,a.event.test());complete(repository.announced(a.event.id()),v->{});}
        });
    }
    private record Candidate(ServerPlayer player,LegendaryConfig.Spawn rule,BlockPos position){}
    public void attempt(CommandSourceStack source,String testSpecies,boolean guarantee){
        if(!ready||busy){reply(source,"Sistema indisponível ou tentativa em andamento.");return;}
        if(testSpecies==null&&(!state.enabled()||!config.main.enabled)){reply(source,"Sistema desativado.");return;}
        if(source==null&&testSpecies==null&&active.size()>=config.main.maxActiveLegendaryEvents){recordSkipped(source,testSpecies,"ACTIVE_LIMIT");return;}
        busy=true;long now=System.currentTimeMillis();UUID id=UUID.randomUUID();boolean automatic=source==null&&testSpecies==null;
        long expected=state.next(),next=LegendaryRules.recover(expected,now,config.main.intervalMillis()).next();
        complete(repository.claim(id,expected,now,next,automatic?"AUTO":testSpecies==null?"FORCE":"TEST"),won->{
            if(!won){busy=false;complete(repository.load(now+config.main.intervalMillis(),config.main.enabled),s->state=s);return;}
            if(automatic)state=new LegendaryRepository.State(now,next,false,state.enabled(),state.missed());
            choose(id,source,testSpecies,guarantee,now);
        });
    }
    private void recordSkipped(CommandSourceStack source,String test,String outcome){
        // Still consume an automatic cycle when an active event blocks a new appearance.
        busy=true;long now=System.currentTimeMillis();long expected=state.next(),next=LegendaryRules.recover(expected,now,config.main.intervalMillis()).next();UUID id=UUID.randomUUID();boolean automatic=source==null&&test==null;
        complete(repository.claim(id,expected,now,next,automatic?"AUTO":test==null?"FORCE":"TEST"),won->{if(won){if(automatic)state=new LegendaryRepository.State(now,next,false,state.enabled(),state.missed());end(id,source,outcome);}else busy=false;});
    }
    private void choose(UUID id,CommandSourceStack source,String test,boolean guarantee,long now){
        if(server.getPlayerList().getPlayers().stream().noneMatch(p->eligibility.player(p,config))){end(id,source,"NO_PLAYERS");return;}
        if(!guarantee&&random.nextDouble()>=config.main.globalSpawnChance){end(id,source,"CHANCE_FAILED");return;}
        // Species are weighted once, regardless of how many players qualify for a species.
        Map<LegendaryConfig.Spawn,List<Candidate>> candidates=new LinkedHashMap<>();
        for(var e:config.spawns){
            if(test!=null?!e.species.equals(test)||config.unavailable.containsKey(e.species)||!e.acquisition.equals("AUTOMATIC"):!config.usable(e))continue;
            List<Candidate> list=new ArrayList<>();
            for(var p:server.getPlayerList().getPlayers()){
                if(!eligibility.player(p,config)||!eligibility.environment(p.serverLevel(),p.blockPosition(),e,config))continue;
                BlockPos pos=eligibility.position(p,e,config,random);if(pos!=null)list.add(new Candidate(p,e,pos));
            }
            if(!list.isEmpty())candidates.put(e,list);
        }
        if(candidates.isEmpty()){end(id,source,"NO_CANDIDATES");return;}
        List<LegendaryConfig.Spawn> species=new ArrayList<>(candidates.keySet());
        int index=LegendaryRules.select(species.stream().map(e->e.weight*config.rarities.get(e.rarity)).toList(),random);
        if(index<0){end(id,source,"NO_WEIGHT");return;}
        var list=candidates.get(species.get(index));Candidate candidate=list.get(random.nextInt(list.size()));
        spawn(id,source,candidate,test!=null,now);
    }
    private void spawn(UUID id,CommandSourceStack source,Candidate candidate,boolean test,long now){
        var p=candidate.player();var e=candidate.rule();var pos=candidate.position();var w=p.serverLevel();
        if(!eligibility.player(p,config)||!eligibility.environment(w,pos,e,config)){end(id,source,"CONDITIONS_CHANGED");return;}
        PokemonEntity entity;
        try{
            entity=PokemonProperties.Companion.parse(e.species+" level="+e.level).createEntity(w);
            if(!entity.getPokemon().getSpecies().getResourceIdentifier().getPath().equals(e.species)){end(id,source,"SPECIES_MISMATCH");return;}
            entity.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,random.nextFloat()*360,0);protect(entity);
            if(!w.noCollision(entity)||!w.getWorldBorder().isWithinBounds(entity.getBoundingBox())||entity.getBoundingBox().minY<w.getMinBuildHeight()||entity.getBoundingBox().maxY>w.getMaxBuildHeight()){end(id,source,"UNSAFE_POSITION");return;}
        }catch(Exception error){AtlasMod.LOGGER.warn("Espécie lendária rejeitada na criação: {}",e.species,error);end(id,source,"CREATE_FAILED");return;}
        entity.getPokemon().getPersistentData().putUUID("atlasLegendaryEvent",id);
        String biome=w.getBiome(pos).unwrapKey().orElseThrow().location().toString();
        var event=new LegendaryRepository.Event(id,entity.getPokemon().getUuid(),entity.getUUID(),e.species,w.dimension().location().toString(),biome,entity.getX(),entity.getY(),entity.getZ(),now,now+config.main.despawnAfterMinutes*60000L,"PENDING",test);
        complete(repository.reserve(event,config.main.maxActiveLegendaryEvents,source!=null||test),reserved->{
            if(!reserved){end(id,source,"ACTIVE_LIMIT");return;}
            if(!eligibility.player(p,config)||p.serverLevel()!=w||!eligibility.environment(w,pos,e,config)||!w.noCollision(entity)||!w.hasChunkAt(pos)){
                complete(repository.transition(id,"FAILED"),v->end(id,source,"CONDITIONS_CHANGED"));return;}
            Active a=new Active(event,e);a.entity=entity;a.chunk=entity.chunkPosition();a.lastMove=now;active.put(event.pokemon(),a);
            w.getChunkSource().addRegionTicket(TICKET,a.chunk,2,id);
            if(!w.addFreshEntity(entity)){complete(repository.transition(id,"FAILED"),v->{release(a);end(id,source,"SPAWN_FAILED");});return;}
            complete(repository.transition(id,"ACTIVE"),v->{
                if(!entity.isRemoved()&&w.getEntity(entity.getUUID())==entity)announcements.spawn(server,config,e,entity.getPokemon(),biome,test);
                end(id,source,"SPAWNED");
            });
        });
    }
    private void end(UUID id,CommandSourceStack source,String outcome){complete(repository.outcome(id,outcome,""),v->{busy=false;reply(source,"Resultado da tentativa: "+outcome);AtlasMod.LOGGER.info("Tentativa lendária {}: {}",id,outcome);});}
    private void reply(CommandSourceStack source,String text){if(source!=null)source.sendSuccess(()->Component.literal(text),false);}
    public int preview(CommandSourceStack source){
        if(!ready||stopped){source.sendFailure(Component.literal("Sistema de lendários indisponível."));return 0;}
        var player=source.getPlayer();if(player==null){source.sendFailure(Component.literal("Use este comando dentro do jogo."));return 0;}
        var world=player.serverLevel();var position=player.blockPosition();
        String biome=world.getBiome(position).unwrapKey().map(key->key.location().toString()).orElse("");
        reply(source,"✦ ATLAS • Lendários possíveis neste local");
        source.sendSuccess(()->Component.literal("Bioma: ").append(Component.translatable("biome."+biome.replace(':','.'))),false);
        if(!config.main.allowedWorlds.contains(world.dimension().location().toString())){reply(source,"Esta dimensão não participa das aparições lendárias.");return 1;}
        var candidates=config.spawns.stream().filter(config::usable)
            .filter(rule->rule.acquisition.equals("AUTOMATIC")&&eligibility.environment(world,position,rule,config))
            .sorted(Comparator.comparing(rule->rule.displayName)).toList();
        if(candidates.isEmpty()){reply(source,"Nenhum lendário elegível neste bioma, altitude, horário e clima.");return 1;}
        double total=candidates.stream().mapToDouble(rule->rule.weight*config.rarities.get(rule.rarity)).sum();
        for(var rule:candidates){double percent=rule.weight*config.rarities.get(rule.rarity)*100/total;
            source.sendSuccess(()->Component.literal("• "+rule.displayName+" — ").withStyle(style->style.withColor(0xFFFFFF))
                .append(com.cobblemon.mod.common.command.CheckSpawnsCommand.INSTANCE.applyColour(Component.literal(String.format(java.util.Locale.forLanguageTag("pt-BR"),"%.2f",percent)+"%"),(float)percent))
                .append(Component.literal(" do peso local").withStyle(style->style.withColor(0xAAAAAA))),false);}
        if(!eligibility.player(player,config))reply(source,"Você não está elegível agora: verifique modo Survival e ausência de batalha.");
        if(!state.enabled()||!config.main.enabled)reply(source,"Aparições automáticas estão desativadas.");
        return 1;
    }
    public int command(CommandSourceStack source,String action,String species){
        if(action.equals("force")||action.equals("test")){reply(source,"Tentativa solicitada; resultado será registrado no histórico do servidor.");attempt(source,species,action.equals("test"));return 1;}
        if(action.equals("reload")){
            if(busy){reply(source,"Aguarde a tentativa atual.");return 0;}
            try{config=LegendaryConfig.load(source.getServer());if(!ready)start(source.getServer());reply(source,"Configurações recarregadas; eventos ativos preservados.");}catch(Exception e){source.sendFailure(Component.literal("Configuração rejeitada: "+e.getMessage()));}return 1;
        }
        if(!ready){source.sendFailure(Component.literal("Sistema de lendários indisponível; consulte os logs."));return 0;}
        if(action.equals("enable")||action.equals("disable")){
            if(busy){reply(source,"Aguarde a operação atual.");return 0;}
            if(action.equals("enable")&&!config.main.enabled){reply(source,"Habilite enabled no config.json e recarregue primeiro.");return 0;}
            reply(source,"Alteração de estado solicitada.");
            busy=true;complete(repository.enabled(action.equals("enable")),s->{state=s;busy=false;reply(source,"Sistema "+(s.enabled()?"ativado":"desativado")+". Eventos ativos preservados.");});return 1;
        }
        if(action.equals("list")){for(var e:config.spawns)reply(source,e.displayName+": "+(config.unavailable.containsKey(e.species)?config.unavailable.get(e.species):config.usable(e)?"habilitado":"desativado"));return 1;}
        if(action.equals("active")){reply(source,"Eventos ativos: "+active.size());for(var a:active.values())reply(source,a.rule.displayName+" • "+a.event.id()+" • "+(a.finishing?"Encerrando":a.entity==null?"Recuperando":"ACTIVE"));return 1;}
        long seconds=Math.max(0,(state.next()-System.currentTimeMillis())/1000);
        reply(source,"ATLAS COBBLEMON • Próxima tentativa: "+seconds/3600+"h "+seconds%3600/60+"min");
        reply(source,"Mundo permitido: Exploração • Sistema: "+(state.enabled()&&config.main.enabled?"Ativo":"Desativado"));
        if(action.equals("status"))reply(source,"Chance global: "+config.main.globalSpawnChance*100+"% • Ativos: "+active.size()+" • Ciclos perdidos: "+state.missed());
        return 1;
    }
    public void stop(){stopped=true;ready=false;bossBars.clear();for(Active a:List.copyOf(active.values())){if(a.entity!=null&&!a.entity.isRemoved()&&!a.finishing)repository.moved(a.event.id(),a.entity.getX(),a.entity.getY(),a.entity.getZ());var w=world(a.event.dimension());if(w!=null&&a.chunk!=null)w.getChunkSource().removeRegionTicket(TICKET,a.chunk,2,a.event.id());}active.clear();repository.close();}
}
