package io.atlas.modules.rank.service;

import io.atlas.modules.rank.repository.PlayerPermissionRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerPermissionService {
    private record Key(UUID uuid, String permission) {}
    private record Entry(boolean enabled, long expires) {}
    private final PlayerPermissionRepository repository = new PlayerPermissionRepository();
    private final Map<Key, Entry> cache = new HashMap<>();

    public boolean hasPermission(UUID uuid, String permission) {
        long now = System.nanoTime();
        Key key = new Key(uuid, permission);
        Entry entry = cache.get(key);
        if (entry != null && entry.expires() > now) return entry.enabled();
        if (cache.size() > 512) cache.entrySet().removeIf(value -> value.getValue().expires() <= now);
        boolean enabled = repository.hasPermission(uuid, permission);
        cache.put(key, new Entry(enabled, now + 5_000_000_000L));
        return enabled;
    }

    public void forget(UUID uuid) { cache.keySet().removeIf(key -> key.uuid().equals(uuid)); }
}
