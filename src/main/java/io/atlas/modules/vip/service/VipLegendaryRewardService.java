package io.atlas.modules.vip.service;
import io.atlas.AtlasMod;
import io.atlas.mixin.*;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.legendary.config.LegendaryModelRegistry;
import io.atlas.modules.vip.repository.VipLegendaryRewardRepository;
import io.atlas.modules.vip.repository.VipLegendaryRewardRepository.Reward;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.*;
import com.cobblemon.mod.common.api.storage.PokemonStore;
import com.cobblemon.mod.common.api.storage.factory.FileBackedPokemonStoreFactory;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
/** Durable reward outbox; uncertainty never regenerates a Pokémon. */
public final class VipLegendaryRewardService {
 private final VipLegendaryRewardRepository repo=new VipLegendaryRewardRepository();
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"atlas-vip-legendary");t.setDaemon(true);return t;});
 private MinecraftServer server;private List<String> pool=List.of();
 public void start(MinecraftServer server){this.server=server;try(var in=VipLegendaryRewardService.class.getResourceAsStream("/atlas-legendary/legendary-spawns.json")){
  var models=LegendaryModelRegistry.available();var names=new ArrayList<String>();
  for(var el:JsonParser.parseReader(new InputStreamReader(Objects.requireNonNull(in),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray()){
   String name=el.getAsJsonObject().get("species").getAsString();var species=PokemonSpecies.getByName(name);
   if(!name.equals("phione")&&species!=null&&species.getImplemented()&&models.contains(name))names.add(name);
  }
  pool=names.stream().distinct().sorted().toList();AtlasMod.LOGGER.info("VIP 3: {} espécies compatíveis no prêmio, Phione excluído; taxa shiny do servidor.",pool.size());
  worker.scheduleWithFixedDelay(this::poll,10,15,TimeUnit.SECONDS);
 }catch(Exception e){AtlasMod.LOGGER.error("Prêmios lendários VIP suspensos; compras preservadas.",e);}}
 private <T>T main(Supplier<T> action)throws Exception{var future=new CompletableFuture<T>();server.execute(()->{try{future.complete(action.get());}catch(Throwable e){future.completeExceptionally(e);}});return future.get(20,TimeUnit.SECONDS);}
 private ServerPlayer player(Reward r){var p=server.getPlayerList().getPlayer(r.player());return p!=null&&!p.hasDisconnected()&&AuthModule.getAuthService().isAuthenticated(r.player())?p:null;}
 private String generate(Reward r){var p=player(r);if(p==null||pool.isEmpty())return null;String species=pool.get(ThreadLocalRandom.current().nextInt(pool.size()));var props=PokemonProperties.Companion.parse(species+" level=70");float rate=Cobblemon.INSTANCE.getConfig().getShinyRate();if(!Float.isFinite(rate)||rate<1)throw new IllegalStateException("Invalid shiny rate");props.setShiny(ThreadLocalRandom.current().nextDouble()<1.0/rate);var pokemon=props.create(p);pokemon.setUuid(r.pokemon());pokemon.setOriginalTrainer(r.player());return pokemon.saveToJSON(server.registryAccess(),new JsonObject()).toString();}
 private record Saved(List<Future<?>> barriers,String name,boolean shiny){}
 private Saved deliver(Reward r,boolean recovering){var p=player(r);if(p==null)return null;
  var storage=Cobblemon.INSTANCE.getStorage();var party=storage.getParty(p);var pc=storage.getPC(p);Pokemon existing=party.get(r.pokemon());if(existing==null)existing=pc.get(r.pokemon());
  if(existing==null&&recovering)throw new IllegalStateException("UNCERTAIN_DELIVERY");
  // Verify durable storage access before inserting anything.
  List<FileBackedPokemonStoreFactory<?>> factories=new ArrayList<>();for(var factory:((PokemonStoreManagerAccessor)storage).atlas$factories())if(factory instanceof FileBackedPokemonStoreFactory<?> file&&(file.isCached(party)||file.isCached(pc)))factories.add(file);
  if(factories.stream().noneMatch(file->file.isCached(party))||factories.stream().noneMatch(file->file.isCached(pc)))throw new IllegalStateException("Unsupported durable storage");
  if(existing==null){if(party.getFirstAvailablePosition()==null&&pc.getFirstAvailablePosition()==null)return null;existing=new Pokemon().loadFromJSON(server.registryAccess(),JsonParser.parseString(r.data()).getAsJsonObject());if(!existing.getUuid().equals(r.pokemon()))throw new IllegalStateException("Reward UUID mismatch");if(!party.add(existing))return null;}
  var barriers=new ArrayList<Future<?>>();for(var file:factories){if(file.isCached(party))barriers.add(save(file,party));if(file.isCached(pc))barriers.add(save(file,pc));}
  return new Saved(barriers,existing.getSpecies().getName(),existing.getShiny());
 }
 @SuppressWarnings({"rawtypes","unchecked"})
 private Future<?> save(FileBackedPokemonStoreFactory<?> file,PokemonStore<?> store){var accessor=(PokemonFileStoreAccessor)file;com.cobblemon.mod.common.api.storage.adapter.flatfile.FileStoreAdapter adapter=accessor.atlas$adapter();Object snapshot=adapter.serialize(store,server.registryAccess());return accessor.atlas$saveExecutor().submit(()->adapter.save(store.getClass(),store.getUuid(),snapshot));}
 private void poll(){try{for(var reward:repo.pending()){
  try{
   if(!main(()->player(reward)!=null))continue;
   Reward r=reward;
   if(r.state().equals("PENDING")){String data=main(()->generate(reward));if(data==null||!repo.prepare(r.delivery(),data))continue;r=new Reward(r.delivery(),r.player(),r.pokemon(),"READY",data);}
   final Reward current=r;boolean recovering=r.state().equals("DELIVERING");
   if(!recovering&&!repo.transition(r.delivery(),"READY","DELIVERING"))continue;
   Saved saved=main(()->deliver(current,recovering));
   if(saved==null){if(!recovering)repo.transition(r.delivery(),"DELIVERING","READY");continue;}
   for(var barrier:saved.barriers())barrier.get(20,TimeUnit.SECONDS);
   if(repo.transition(r.delivery(),"DELIVERING","DELIVERED"))main(()->{var p=player(current);if(p!=null)p.displayClientMessage(Component.literal("§aPrêmio VIP 3 recebido: §f"+saved.name()+(saved.shiny()?" §6★ Shiny!":"")+"§a. Consulte sua equipe ou PC."),false);return true;});
  }catch(Exception e){Throwable cause=e;while(cause.getCause()!=null)cause=cause.getCause();if("UNCERTAIN_DELIVERY".equals(cause.getMessage())){repo.transition(reward.delivery(),"DELIVERING","REVIEW");AtlasMod.LOGGER.error("Prêmio VIP {} precisa de conferência; não será regenerado.",reward.delivery());}else AtlasMod.LOGGER.warn("Prêmio VIP {} pendente; sorteio preservado.",reward.delivery());}
 }}catch(Exception e){AtlasMod.LOGGER.warn("Fila de prêmios VIP temporariamente indisponível.");}}
 public void stop(){worker.shutdownNow();}
}
