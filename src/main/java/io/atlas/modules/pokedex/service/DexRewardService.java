package io.atlas.modules.pokedex.service;
import io.atlas.AtlasMod;
import io.atlas.mixin.*;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.pokedex.repository.DexRewardRepository;
import io.atlas.modules.pokedex.repository.DexRewardRepository.Reward;
import io.atlas.modules.pokedex.model.DexRewardPlan;
import io.atlas.modules.legendary.config.LegendaryModelRegistry;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.*;
import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.api.storage.factory.FileBackedPokemonStoreFactory;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
public final class DexRewardService {
 private final DexRewardRepository repository=new DexRewardRepository();
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"atlas-dex-rewards");t.setDaemon(true);return t;});
 private MinecraftServer server;private List<String> legends=List.of(),ordinary=List.of();
 private Map<String,List<String>> starterPools=Map.of();
 private final Set<UUID> requests=ConcurrentHashMap.newKeySet();
 private final Map<String,Integer> unlocked=new HashMap<>();
 public void start(MinecraftServer server){this.server=server;
  try(var in=DexRewardService.class.getResourceAsStream("/atlas-legendary/legendary-spawns.json")){
   var configured=new HashSet<String>();for(var entry:JsonParser.parseReader(new java.io.InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray())configured.add(entry.getAsJsonObject().get("species").getAsString());
   var models=LegendaryModelRegistry.available();
   legends=PokemonSpecies.getImplemented().stream().filter(s->!s.getResourceIdentifier().getPath().equals("phione")&&models.contains(s.getResourceIdentifier().getPath())&&(configured.contains(s.getResourceIdentifier().getPath())||s.getLabels().contains("legendary")||s.getLabels().contains("mythical"))).map(s->s.getResourceIdentifier().toString()).distinct().sorted().toList();
   ordinary=PokemonSpecies.getImplemented().stream().filter(s->models.contains(s.getResourceIdentifier().getPath())&&!configured.contains(s.getResourceIdentifier().getPath())&&!s.getLabels().contains("legendary")&&!s.getLabels().contains("mythical")).map(s->s.getResourceIdentifier().toString()).distinct().sorted().toList();
   var starterModels=io.atlas.modules.pokedex.model.GenerationStarterModels.available();var pools=new HashMap<String,List<String>>();for(var route:io.atlas.modules.pokedex.model.GenerationRoutePlan.ROUTES){var starters=io.atlas.modules.pokedex.model.GenerationRoutePlan.starters(route).stream().filter(starterModels::contains).filter(name->PokemonSpecies.getByName(name)!=null&&PokemonSpecies.getByName(name).getImplemented()).map(name->PokemonSpecies.getByName(name).getResourceIdentifier().toString()).toList();if(starters.isEmpty())throw new IllegalStateException("Missing starter pool "+route);pools.put(route,starters);}starterPools=Map.copyOf(pools);AtlasMod.LOGGER.info("Academia: {} iniciais compatíveis nas nove gerações.",starterPools.values().stream().mapToInt(List::size).sum());
   if(legends.isEmpty()||ordinary.isEmpty())throw new IllegalStateException("Reward pools empty");
   worker.scheduleWithFixedDelay(this::poll,5,5,TimeUnit.SECONDS);AtlasMod.LOGGER.info("Academia: {} lendários e {} espécies comuns compatíveis nos prêmios.",legends.size(),ordinary.size());
  }catch(Exception e){AtlasMod.LOGGER.error("Prêmios da Academia suspensos; progresso preservado.",e);}
 }
 private <T>T main(Supplier<T> action)throws Exception{var f=new CompletableFuture<T>();server.execute(()->{try{f.complete(action.get());}catch(Throwable e){f.completeExceptionally(e);}});return f.get(20,TimeUnit.SECONDS);}
 private ServerPlayer player(UUID id){var p=server.getPlayerList().getPlayer(id);return p!=null&&!p.hasDisconnected()&&AuthModule.getAuthService().isAuthenticated(id)?p:null;}
 public void unlock(ServerPlayer player,int milestone){unlock(player,"main",milestone);}
 public void unlock(ServerPlayer player,String route,int milestone){String key=player.getUUID()+":"+route;if(milestone<10||unlocked.getOrDefault(key,0)>=milestone)return;UUID id=player.getUUID();unlocked.put(key,milestone);worker.execute(()->{try{repository.unlock(id,route,milestone);}catch(Exception e){server.execute(()->unlocked.remove(key));AtlasMod.LOGGER.warn("Falha ao liberar prêmios da Academia",e);}});}
 public void remove(UUID id){unlocked.keySet().removeIf(k->k.startsWith(id+":"));}
 private void request(ServerPlayer p,Runnable action){if(!AuthModule.getAuthService().isAuthenticated(p.getUUID())||!requests.add(p.getUUID()))return;worker.execute(()->{try{action.run();}catch(Exception e){message(p.getUUID(),"§cPrêmios temporariamente indisponíveis. Tente novamente.");AtlasMod.LOGGER.warn("Falha na solicitação de prêmio da Academia",e);}finally{requests.remove(p.getUUID());}});}
 private void message(UUID id,String text){server.execute(()->{var p=player(id);if(p!=null)p.sendSystemMessage(Component.literal(text));});}
 public int open(ServerPlayer p){return open(p,0,"HOME",0,null);}
 public int show(ServerPlayer p){return open(p);}
 public int open(ServerPlayer p,int mark,String mode,int page,String selected){return open(p,"main",mark,mode,page,selected);}
 public int open(ServerPlayer p,String route,int mark,String mode,int page,String selected){
  var mainProgress=io.atlas.modules.pokedex.PokedexModule.service().progress(p);
  var typeProgress=io.atlas.modules.pokedex.PokedexModule.types().progress(p.getUUID());
  var progress=route.equals("main")?mainProgress:typeProgress.stream().filter(t->t.type().equals(route)).findFirst().map(t->new io.atlas.modules.pokedex.repository.PokedexRepository.Progress(t.milestone(),t.caught(),t.total(),t.completed())).orElse(new io.atlas.modules.pokedex.repository.PokedexRepository.Progress(0,0,0,null));
  request(p,()->{var rewards=repository.list(p.getUUID(),route);List<String> options=switch(mode){case "MINT"->DexRewardPlan.MINTS.stream().sorted().toList();case "LEGENDARY"->legends;case "REGION"->List.of("kanto","johto","hoenn","sinnoh","unova","kalos","alola","galar","paldea","outros");case "TYPE"->List.of("normal","fire","water","electric","grass","ice","fighting","poison","ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy");default->List.of();};
   server.execute(()->{var current=player(p.getUUID());if(current!=null)current.openMenu(new net.minecraft.world.SimpleMenuProvider((id,inventory,viewer)->(mode.equals("TYPE")||mode.equals("GENERATION"))?new io.atlas.modules.pokedex.menu.TypeRoutesMenu(id,inventory,typeProgress,mode.equals("GENERATION")):new io.atlas.modules.pokedex.menu.DexRewardsMenu(id,inventory,progress,rewards,mark,mode,options,page,selected,route),Component.literal("Academia Atlas — Pokédex")));});
  });return 1;
 }
 public int claim(ServerPlayer p,int mark){return claim(p,"main",mark);}
 public int claim(ServerPlayer p,String route,int mark){request(p,()->{repository.claim(p.getUUID(),route,mark);message(p.getUUID(),"§aResgate solicitado. Abra /dex para acompanhar e escolher seus prêmios.");});return 1;}
 public int mint(ServerPlayer p,int mark,String nature){return mint(p,"main",mark,nature);}
 public int mint(ServerPlayer p,String route,int mark,String nature){String normalized=nature.toLowerCase(Locale.ROOT).replace("_mint","");if(!DexRewardPlan.MINTS.contains(normalized)){p.sendSystemMessage(Component.literal("§cNature inválida. Use /academia mints."));return 0;}request(p,()->{boolean picked=repository.choice(p.getUUID(),route,mark,"MINT","cobblemon:"+normalized+"_mint");message(p.getUUID(),picked?"§aMint escolhida. Entrega solicitada.":"§cNão há escolha de mint disponível nesse marco.");});return 1;}
 public int chooseLegend(ServerPlayer p,String name){var species=PokemonSpecies.getByName(name);if(species==null||!legends.contains(species.getResourceIdentifier().toString())){p.sendSystemMessage(Component.literal("§cEspécie indisponível. Consulte /academia lendarios."));return 0;}request(p,()->{boolean picked=repository.choice(p.getUUID(),100,"LEGENDARY_CHOICE",species.getResourceIdentifier().toString());message(p.getUUID(),picked?"§aLendário shiny escolhido. Entrega solicitada.":"§cNão há escolha lendária disponível.");});return 1;}
 public int options(ServerPlayer p,boolean mint){p.sendSystemMessage(Component.literal("§e"+String.join(", ",mint?DexRewardPlan.MINTS.stream().sorted().toList():legends)));return 1;}
 public String generate(Reward r){return generateFor(r,player(r.player()));}
 public String generateFor(Reward r,ServerPlayer p){if(p==null)return null;var payload=new JsonObject();
  if(r.kind().equals("ITEM")||r.kind().equals("MINT")){var key=ResourceLocation.parse(r.value());var item=BuiltInRegistries.ITEM.get(key);if(item==Items.AIR||r.amount()>item.getDefaultMaxStackSize())throw new IllegalStateException("Missing item or invalid stack "+r.value());payload.addProperty("item",r.value());payload.addProperty("amount",r.amount());return payload.toString();}
  boolean ordinaryReward=Set.of("SHINY","RANDOM","HA").contains(r.kind());var ordinaryPool=r.route().equals("main")?ordinary:io.atlas.modules.pokedex.model.GenerationRoutePlan.ROUTES.contains(r.route())?starterPools.getOrDefault(r.route(),List.of()):ordinary.stream().filter(name->!io.atlas.modules.pokedex.model.GenerationRoutePlan.ROUTES.contains(r.route())||io.atlas.modules.pokedex.model.GenerationRoutePlan.starters(r.route()).contains(ResourceLocation.parse(name).getPath())).filter(name->io.atlas.modules.pokedex.service.TypeRouteService.matches(PokemonSpecies.getByIdentifier(ResourceLocation.parse(name)),r.route())).toList();if(r.kind().equals("HA")) ordinaryPool=ordinaryPool.stream().filter(name->java.util.stream.StreamSupport.stream(PokemonSpecies.getByIdentifier(ResourceLocation.parse(name)).getAbilities().spliterator(),false).anyMatch(a->a instanceof com.cobblemon.mod.common.pokemon.abilities.HiddenAbility)).toList();if(ordinaryReward&&ordinaryPool.isEmpty())throw new IllegalStateException("No compatible species for type reward");String species=r.kind().equals("LEGENDARY_CHOICE")?r.value():(ordinaryReward?ordinaryPool:legends).get(ThreadLocalRandom.current().nextInt((ordinaryReward?ordinaryPool:legends).size()));
  var properties=PokemonProperties.Companion.parse(species+" level="+(ordinaryReward?50:70));boolean shiny=r.kind().equals("SHINY")||r.kind().equals("LEGENDARY_CHOICE");if(!shiny){float rate=Cobblemon.INSTANCE.getConfig().getShinyRate();if(!Float.isFinite(rate)||rate<1)throw new IllegalStateException("Invalid shiny rate");shiny=ThreadLocalRandom.current().nextDouble()<1.0/rate;}properties.setShiny(shiny);var pokemon=properties.create(p);if(r.kind().equals("HA")){pokemon=guaranteedHa(ordinaryPool,p,shiny);species=pokemon.getSpecies().getResourceIdentifier().toString();}if(!pokemon.getSpecies().getResourceIdentifier().toString().equals(species))throw new IllegalStateException("Generated reward species mismatch");pokemon.setUuid(r.id());pokemon.setOriginalTrainer(r.player());if(r.kind().equals("HA")){var ha=new com.cobblemon.mod.common.pokemon.properties.HiddenAbilityProperty(true);if(!ha.matches(pokemon))throw new IllegalStateException("Guaranteed HA unavailable");pokemon.setTradeable(false);pokemon.getPersistentData().putBoolean("atlasGuaranteedHaReward",true);pokemon.getPersistentData().putUUID("atlasRewardOwner",r.player());}
  var stats=new Stats[]{Stats.HP,Stats.ATTACK,Stats.DEFENCE,Stats.SPECIAL_ATTACK,Stats.SPECIAL_DEFENCE,Stats.SPEED};int[] values=new int[6];for(int i=0;i<6;i++)values[i]=pokemon.getIvs().get(stats[i]);int[] fixed=io.atlas.modules.legendary.model.LegendaryIvRules.ensure(values,ThreadLocalRandom.current());for(int i=0;i<6;i++)if(values[i]!=fixed[i])pokemon.setIV(stats[i],fixed[i]);return pokemon.saveToJSON(server.registryAccess(),new JsonObject()).toString();
 }
 private Pokemon guaranteedHa(List<String> pool,ServerPlayer player,boolean shiny){
  var candidates=new ArrayList<>(pool);Collections.shuffle(candidates);
  for(String name:candidates){var props=PokemonProperties.Companion.parse(name+" level=50");props.setShiny(shiny);var pokemon=props.create(player);var ha=new com.cobblemon.mod.common.pokemon.properties.HiddenAbilityProperty(true);ha.apply(pokemon);if(ha.matches(pokemon))return pokemon;}
  throw new IllegalStateException("No form supports guaranteed HA");
 }
 public record Saved(List<Future<?>> barriers,String name){}
 public Saved deliver(Reward r,boolean recovery){var p=player(r.player());if(p==null||com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer(p)!=null)return null;
  if(r.kind().equals("ITEM")||r.kind().equals("MINT")){
   ItemStack existing=null;for(int i=0;i<p.getInventory().getContainerSize();i++){var stack=p.getInventory().getItem(i);var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();if(tag.hasUUID("atlasDexReward")&&tag.getUUID("atlasDexReward").equals(r.id())){existing=stack;break;}}
   if(existing==null&&recovery)throw new IllegalStateException("UNCERTAIN_DELIVERY");
   if(existing==null){int free=p.getInventory().getFreeSlot();if(free<0)return null;var data=JsonParser.parseString(r.data()).getAsJsonObject();var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.get("item").getAsString()));if(item==Items.AIR)throw new IllegalStateException("Item unavailable");existing=new ItemStack(item,data.get("amount").getAsInt());var tag=new CompoundTag();tag.putUUID("atlasDexReward",r.id());existing.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));p.getInventory().setItem(free,existing);p.getInventory().setChanged();p.containerMenu.broadcastChanges();}
   ((PlayerListSaveInvoker)p.getServer().getPlayerList()).atlas$savePlayer(p);
   try{var file=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat");var disk=net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.create(16777216));boolean found=false;for(var entry:disk.getList("Inventory",10)){var item=(CompoundTag)entry;var tag=item.getCompound("components").getCompound("minecraft:custom_data");if(tag.hasUUID("atlasDexReward")&&tag.getUUID("atlasDexReward").equals(r.id())){found=true;break;}}if(!found)throw new IllegalStateException("Item not durably saved");}catch(java.io.IOException error){throw new IllegalStateException("Item save verification failed",error);}
   return new Saved(List.of(),existing.getHoverName().getString());
  }
  var storage=Cobblemon.INSTANCE.getStorage();var party=storage.getParty(p);var pc=storage.getPC(p);Pokemon existing=party.get(r.id());if(existing==null)existing=pc.get(r.id());if(existing==null&&recovery)throw new IllegalStateException("UNCERTAIN_DELIVERY");
  var factories=new ArrayList<FileBackedPokemonStoreFactory<?>>();for(var factory:((PokemonStoreManagerAccessor)storage).atlas$factories())if(factory instanceof FileBackedPokemonStoreFactory<?> file&&(file.isCached(party)||file.isCached(pc)))factories.add(file);
  if(factories.stream().noneMatch(file->file.isCached(party))||factories.stream().noneMatch(file->file.isCached(pc)))throw new IllegalStateException("Unsupported durable storage");
  if(existing==null){if(party.getFirstAvailablePosition()==null&&pc.getFirstAvailablePosition()==null)return null;existing=new Pokemon().loadFromJSON(server.registryAccess(),JsonParser.parseString(r.data()).getAsJsonObject());if(!existing.getUuid().equals(r.id()))throw new IllegalStateException("Reward identity mismatch");if(!party.add(existing))return null;}
  var barriers=new ArrayList<Future<?>>();for(var file:factories){if(file.isCached(party))barriers.add(save(file,party));if(file.isCached(pc))barriers.add(save(file,pc));}return new Saved(barriers,existing.getSpecies().getName()+(existing.getShiny()?" ★ Shiny":""));
 }
 @SuppressWarnings({"rawtypes","unchecked"}) private Future<?> save(FileBackedPokemonStoreFactory<?> file,PokemonStore<?> store){var accessor=(PokemonFileStoreAccessor)file;com.cobblemon.mod.common.api.storage.adapter.flatfile.FileStoreAdapter adapter=accessor.atlas$adapter();Object snapshot=adapter.serialize(store,server.registryAccess());return accessor.atlas$saveExecutor().submit(()->adapter.save(store.getClass(),store.getUuid(),snapshot));}
 private void poll(){try{for(var reward:repository.pending()){try{if(!main(()->player(reward.player())!=null))continue;Reward r=reward;if(r.state().equals("PENDING")){String data=main(()->generate(reward));if(data==null||!repository.prepare(r.id(),data))continue;r=new Reward(r.id(),r.player(),r.milestone(),r.part(),r.kind(),r.value(),r.amount(),"READY",data,r.route());}final Reward current=r;boolean recovering=r.state().equals("DELIVERING");if(!recovering&&!repository.transition(r.id(),"READY","DELIVERING"))continue;var saved=main(()->deliver(current,recovering));if(saved==null){if(!recovering)repository.transition(r.id(),"DELIVERING","READY");continue;}for(var barrier:saved.barriers())barrier.get(20,TimeUnit.SECONDS);if(repository.transition(r.id(),"DELIVERING","DELIVERED"))message(r.player(),"§aPrêmio "+(r.route().equals("main")?"principal":io.atlas.modules.pokedex.model.TypeRoutePlan.name(r.route()))+" "+r.milestone()+"% recebido: §f"+saved.name());}catch(Exception e){Throwable cause=e;while(cause.getCause()!=null)cause=cause.getCause();if("UNCERTAIN_DELIVERY".equals(cause.getMessage()))repository.transition(reward.id(),"DELIVERING","REVIEW");AtlasMod.LOGGER.warn("Prêmio da Academia {} preservado para retry/conferência",reward.id(),e);}}}catch(Exception e){AtlasMod.LOGGER.warn("Fila de prêmios da Academia indisponível",e);}}
 public void stop(){worker.shutdownNow();}
}
