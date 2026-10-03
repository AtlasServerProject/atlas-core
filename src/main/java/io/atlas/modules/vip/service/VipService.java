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
 public int level(UUID uuid){var now=Instant.now();var saved=cache.get(uuid);if(saved==null||now.isAfter(saved.loadedAt().plusSeconds(5))){try{saved=new Cached(repo.find(uuid),now);}catch(Exception e){saved=new Cached(Optional.empty(),now);}cache.put(uuid,saved);if(cache.size()>2000)cache.entrySet().removeIf(x->x.getValue().loadedAt().isBefore(now.minusSeconds(60)));}return saved.balance().map(b->VipLedger.advance(b,now).level()).orElse(0);}
 public Grant grant(Delivery d){var granted=repo.grant(d,Instant.now());cache.clear();return granted;}
 public List<Balance> batch(long after){return repo.batch(after,Instant.now());}
 public void invalidate(UUID uuid){cache.remove(uuid);}
}
