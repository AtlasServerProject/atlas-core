package io.atlas.modules.vip.service;
import io.atlas.modules.vip.model.VipLedger;
import io.atlas.modules.vip.model.VipLedger.*;
import io.atlas.modules.vip.repository.VipRepository;
import io.atlas.AtlasMod;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public final class VipService {
 private final VipRepository repo=new VipRepository();
 private record Cached(Optional<Balance> balance,Instant loadedAt){}
 private final Map<UUID,Cached> cache=new ConcurrentHashMap<>();
 private Cached load(UUID uuid, Instant now){
  var saved=cache.get(uuid);
  if(saved==null||now.isAfter(saved.loadedAt().plusSeconds(5))){
   saved=new Cached(repo.find(uuid),now);cache.put(uuid,saved);
   if(cache.size()>2000)cache.entrySet().removeIf(x->x.getValue().loadedAt().isBefore(now.minusSeconds(60)));
  }
  return saved;
 }
 public int level(UUID uuid){try{var now=Instant.now();return load(uuid,now).balance().map(b->VipLedger.advance(b,now).level()).orElse(0);}catch(Exception e){return 0;}}
 public List<String> status(UUID uuid){
  var now=Instant.now();var balance=load(uuid,now).balance().map(b->VipLedger.advance(b,now));
  if(balance.isEmpty()||balance.get().level()==0)return List.of("§eVocê não possui VIP comprado ativo no Emerald.");
  var b=balance.get();int active=b.level();long[] remaining={b.vip1Ms(),b.vip2Ms(),b.vip3Ms()};
  var lines=new ArrayList<String>();lines.add("§6Seu VIP — Emerald");
  lines.add("§aVIP "+active+" ativo §7• §f"+formatRemaining(remaining[active-1])+" restantes");
  var expiry=b.checkpoint().plusMillis(remaining[active-1]);
  lines.add("§7Termina em: §f"+java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(ZoneId.of("America/Fortaleza")).format(expiry)+" §7(horário de Brasília)");
  for(int level=active-1;level>=1;level--)if(remaining[level-1]>0)lines.add("§eVIP "+level+" pausado §7• §f"+formatRemaining(remaining[level-1])+" preservados");
  if(lines.size()>3)lines.add("§7O maior nível consome tempo; os pausados retomam depois.");
  return List.copyOf(lines);
 }
 public static String formatRemaining(long millis){
  long seconds=millis/1000+(millis%1000>0?1:0);
  return (seconds/86400)+"d "+(seconds%86400/3600)+"h "+(seconds%3600/60)+"min "+(seconds%60)+"s";
 }
 public Grant grant(Delivery d){var granted=repo.grant(d,Instant.now());cache.clear();return granted;}
 public List<Balance> batch(long after){return repo.batch(after,Instant.now());}
 public void invalidate(UUID uuid){cache.remove(uuid);}
}
