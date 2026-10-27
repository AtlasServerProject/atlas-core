package io.atlas.modules.crates.service;

import io.atlas.AtlasMod;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rules.RulesModule;
import io.atlas.modules.crates.repository.CrateRepository;
import io.atlas.modules.crates.repository.CrateRepository.*;
import io.atlas.modules.crates.menu.CrateMenu;
import com.cobblemon.mod.common.api.pokemon.*;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Free test mode, fixed outcomes persisted before delivery; retries never reroll. */
public final class CrateService {
 public record Entry(String properties,String name,boolean shiny,double weight,ItemStack icon) {}
 public record Pool(Box box,List<Entry> entries) {}
 private final CrateRepository repo=new CrateRepository();
 private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(r->{var t=new Thread(r,"atlas-crates");t.setDaemon(true);return t;});
 private final Map<UUID,Long> cooldown=new HashMap<>();
 private final Set<UUID> busy=new HashSet<>();
 private record Spin(int age,int frame,String outcome) {}
 private final Map<UUID,Spin> animation=new HashMap<>();
 private volatile List<Pool> pools=List.of();private MinecraftServer server;private int ticks;
 public void start(MinecraftServer server){this.server=server;worker.execute(()->{try{repo.rotateMonth(month());load();}catch(Exception e){AtlasMod.LOGGER.error("Caixas indisponíveis",e);}});worker.scheduleWithFixedDelay(this::refreshMonth,15,15,TimeUnit.SECONDS);worker.scheduleWithFixedDelay(this::poll,5,3,TimeUnit.SECONDS);}
 private java.time.YearMonth month(){return java.time.YearMonth.now(java.time.ZoneId.of("America/Fortaleza"));}
 private void refreshMonth(){try{if(repo.rotateMonth(month()))load();}catch(Exception e){AtlasMod.LOGGER.error("Rotação mensal não concluída; nova tentativa agendada.",e);}}
 private void load() throws Exception {
  var boxes=repo.boxes();main(()->{var list=new ArrayList<Pool>();for(var b:boxes){var entries=new ArrayList<Entry>();for(var r:b.prizes()){
   var props=PokemonProperties.Companion.parse(r.properties()+" level=70");var species=PokemonSpecies.getByName(r.properties().split(" ")[0]);
   if(species==null||!species.getImplemented()||!Double.isFinite(r.weight())||r.weight()<=0){AtlasMod.LOGGER.warn("Prêmio de caixa omitido no registro ativo: {}",r.properties());continue;}
   var pokemon=props.create();entries.add(new Entry(r.properties(),species.getName(),pokemon.getShiny(),r.weight(),io.atlas.modules.market.service.MarketSpriteIcons.from(pokemon)));
  }if(!entries.isEmpty())list.add(new Pool(b,List.copyOf(entries)));}pools=List.copyOf(list);AtlasMod.LOGGER.info("Caixas com CHAVES VIRTUAIS: {} caixas, {} prêmios carregados (inclui caixa mensal de teste).",pools.size(),pools.stream().mapToInt(p->p.entries.size()).sum());return true;});
 }
 private <T>T main(Supplier<T> fn)throws Exception{var f=new CompletableFuture<T>();server.execute(()->{try{f.complete(fn.get());}catch(Throwable e){f.completeExceptionally(e);}});return f.get(20,TimeUnit.SECONDS);}
 public Pool at(String dimension,BlockPos pos){for(var p:pools){var b=p.box;if(b.dimension().equals(dimension)&&b.x()==pos.getX()&&b.y()==pos.getY()&&b.z()==pos.getZ())return p;}return null;}
 public boolean allowed(ServerPlayer p){return !p.hasDisconnected()&&AuthModule.getAuthService().isAuthenticated(p.getUUID())&&RulesModule.service().hasAccepted(p.getUUID());}
 public void preview(ServerPlayer p,Pool box,int page){if(!allowed(p))return;p.openMenu(new SimpleMenuProvider((id,inv,who)->new CrateMenu(id,inv,this,box,page,false),Component.literal(box.box.name()+" • Prévia")));}
 public void openKeys(ServerPlayer p){if(!allowed(p))return;worker.execute(()->{try{var keys=repo.keys(p.getUUID());server.execute(()->{if(allowed(p)&&server.getPlayerList().getPlayer(p.getUUID())==p)p.openMenu(new SimpleMenuProvider((id,inv,who)->new io.atlas.modules.crates.menu.KeyMenu(id,inv,keys),Component.literal("Compras • Chaves virtuais")));});}catch(Exception e){server.execute(()->p.sendSystemMessage(Component.literal("§cNão foi possível consultar suas chaves.")));}});}
 public void grantKeys(net.minecraft.commands.CommandSourceStack source,UUID target,String identifier,int amount){String actor=source.isPlayer()?source.getPlayer().getUUID().toString():"CONSOLE";worker.execute(()->{try{boolean ok=repo.grant(target,identifier,amount,actor);server.execute(()->source.sendSuccess(()->Component.literal(ok?"§aChaves virtuais adicionadas; consulte /compras.":"§cJogador ou caixa indisponível."),false));}catch(Exception e){server.execute(()->source.sendFailure(Component.literal("Falha ao adicionar chaves.")));}});}
 public void roll(ServerPlayer p,Pool box){
  if(!allowed(p))return;var id=p.getUUID();long now=System.currentTimeMillis();
  if(busy.contains(id)||cooldown.getOrDefault(id,0L)>now){p.sendSystemMessage(Component.literal("§eAguarde a entrega e o intervalo de 10 segundos para abrir outra caixa."));return;}
  var storage=com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage();if(storage.getParty(p).getFirstAvailablePosition()==null&&storage.getPC(p).getFirstAvailablePosition()==null){p.sendSystemMessage(Component.literal("§cLibere espaço na equipe ou no PC."));return;}
  busy.add(id);cooldown.put(id,now+10000);
  var entry=select(box.entries,ThreadLocalRandom.current().nextDouble());var props=PokemonProperties.Companion.parse(entry.properties+" level=70");var pokemon=props.create(p);UUID rewardId=UUID.randomUUID();pokemon.setUuid(rewardId);pokemon.setOriginalTrainer(id);
  var d=new Delivery(UUID.randomUUID(),id,rewardId,box.box.id(),entry.properties,pokemon.saveToJSON(server.registryAccess(),new JsonObject()).toString(),"READY");
  worker.execute(()->{try{boolean reserved=repo.reserve(d);main(()->{if(!reserved){busy.remove(id);p.sendSystemMessage(Component.literal("§eVocê não tem a chave desta caixa, já possui um prêmio pendente ou a seleção foi atualizada. Consulte /compras."));return false;}if(allowed(p)&&server.getPlayerList().getPlayer(id)==p){animation.put(id,new Spin(0,0,entry.properties()));p.openMenu(new SimpleMenuProvider((menu,inv,who)->new CrateMenu(menu,inv,this,box,0,true),Component.literal("Roleta • 1 chave virtual")));}return true;});}catch(Exception e){server.execute(()->{busy.remove(id);p.sendSystemMessage(Component.literal("§cNão foi possível iniciar. Um prêmio já registrado será preservado."));});AtlasMod.LOGGER.warn("Falha ao reservar caixa {}",d.id(),e);}});
 }
 public static Entry select(List<Entry> entries,double random){if(entries.isEmpty()||random<0||random>=1)throw new IllegalArgumentException();double total=entries.stream().mapToDouble(Entry::weight).sum();if(!Double.isFinite(total)||total<=0)throw new IllegalArgumentException();double cursor=random*total;for(var e:entries){if(!Double.isFinite(e.weight)||e.weight<=0)throw new IllegalArgumentException();cursor-=e.weight;if(cursor<0)return e;}return entries.getLast();}
 public void tick(MinecraftServer server){ticks++;var it=animation.entrySet().iterator();while(it.hasNext()){
  var e=it.next();var spin=e.getValue();int age=spin.age()+1,frame=spin.frame();var p=server.getPlayerList().getPlayer(e.getKey());
  if(age>=140||p==null||!allowed(p)){it.remove();continue;}
  if(p.containerMenu instanceof CrateMenu menu&&menu.rolling()){
   if(age==100)menu.stopOn(spin.outcome());
   else if(age<100){int interval=age<40?2:age<70?4:age<90?7:10;if(age%interval==0)menu.animate(++frame);}
  }
  e.setValue(new Spin(age,frame,spin.outcome()));
 }
 if(ticks%1200==0)cooldown.entrySet().removeIf(e->e.getValue()<System.currentTimeMillis());
 }
 private ServerPlayer player(Delivery d){var p=server.getPlayerList().getPlayer(d.player());return p!=null&&allowed(p)?p:null;}
 private record Saved(List<Future<?>> barriers,String name,boolean shiny) {}
 private Saved deliver(Delivery d,boolean recovering){var p=player(d);if(p==null||animation.containsKey(d.player()))return null;
  var storage=com.cobblemon.mod.common.Cobblemon.INSTANCE.getStorage();var party=storage.getParty(p);var pc=storage.getPC(p);Pokemon existing=party.get(d.pokemon());if(existing==null)existing=pc.get(d.pokemon());if(existing==null&&recovering)throw new IllegalStateException("UNCERTAIN_DELIVERY");
  var factories=new ArrayList<com.cobblemon.mod.common.api.storage.factory.FileBackedPokemonStoreFactory<?>>();for(var f:((io.atlas.mixin.PokemonStoreManagerAccessor)storage).atlas$factories())if(f instanceof com.cobblemon.mod.common.api.storage.factory.FileBackedPokemonStoreFactory<?> file&&(file.isCached(party)||file.isCached(pc)))factories.add(file);
  if(factories.stream().noneMatch(f->f.isCached(party))||factories.stream().noneMatch(f->f.isCached(pc)))throw new IllegalStateException("Unsupported durable storage");
  if(existing==null){if(party.getFirstAvailablePosition()==null&&pc.getFirstAvailablePosition()==null)return null;existing=new Pokemon().loadFromJSON(server.registryAccess(),com.google.gson.JsonParser.parseString(d.data()).getAsJsonObject());if(!existing.getUuid().equals(d.pokemon()))throw new IllegalStateException("UUID mismatch");if(!party.add(existing))return null;}
  var barriers=new ArrayList<Future<?>>();for(var f:factories){if(f.isCached(party))barriers.add(save(f,party));if(f.isCached(pc))barriers.add(save(f,pc));}return new Saved(barriers,existing.getSpecies().getName(),existing.getShiny());
 }
 @SuppressWarnings({"rawtypes","unchecked"}) private Future<?> save(com.cobblemon.mod.common.api.storage.factory.FileBackedPokemonStoreFactory<?> file,com.cobblemon.mod.common.api.storage.PokemonStore<?> store){var accessor=(io.atlas.mixin.PokemonFileStoreAccessor)file;com.cobblemon.mod.common.api.storage.adapter.flatfile.FileStoreAdapter adapter=accessor.atlas$adapter();Object snapshot=adapter.serialize(store,server.registryAccess());return accessor.atlas$saveExecutor().submit(()->adapter.save(store.getClass(),store.getUuid(),snapshot));}
 private void poll(){try{for(var d:repo.pending())try{
  if(!main(()->player(d)!=null&&!animation.containsKey(d.player())))continue;boolean recovering=d.state().equals("DELIVERING");if(!recovering&&!repo.transition(d.id(),"READY","DELIVERING"))continue;
  var saved=main(()->deliver(d,recovering));if(saved==null){if(!recovering)repo.transition(d.id(),"DELIVERING","READY");continue;}for(var f:saved.barriers)f.get(20,TimeUnit.SECONDS);
  if(repo.transition(d.id(),"DELIVERING","DELIVERED"))main(()->{busy.remove(d.player());var p=player(d);if(p!=null){if(p.containerMenu instanceof CrateMenu m&&m.rolling())m.reveal(d.properties());p.sendSystemMessage(Component.literal("§aPrêmio da caixa recebido: §f"+saved.name+(saved.shiny?" §6★ Shiny":"")+" §7(nível 70; equipe/PC)."));}return true;});
 }catch(Exception e){Throwable cause=e;while(cause.getCause()!=null)cause=cause.getCause();if("UNCERTAIN_DELIVERY".equals(cause.getMessage())){repo.transition(d.id(),"DELIVERING","REVIEW");AtlasMod.LOGGER.error("Caixa {} em conferência; prêmio não será duplicado.",d.id());}else AtlasMod.LOGGER.warn("Entrega de caixa {} preservada para nova tentativa.",d.id());}}catch(Exception e){AtlasMod.LOGGER.warn("Fila de caixas temporariamente indisponível.");}}
 public void stop(){pools=List.of();worker.shutdownNow();}
}
