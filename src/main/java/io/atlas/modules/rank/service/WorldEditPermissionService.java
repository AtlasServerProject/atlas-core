package io.atlas.modules.rank.service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

/** Grants only WorldEdit nodes; authentication is checked even on a cache hit. */
public final class WorldEditPermissionService {
    public enum Decision { DEFAULT, ALLOW, DENY }
    private record Entry(boolean allowed, long expires) {}
    private final Predicate<UUID> authenticated;
    private final Predicate<UUID> administrator;
    private final LongSupplier clock;
    private final Map<UUID, Entry> cache = new HashMap<>();

    public WorldEditPermissionService(Predicate<UUID> authenticated, Predicate<UUID> administrator,
                                     LongSupplier clock) {
        this.authenticated = authenticated;
        this.administrator = administrator;
        this.clock = clock;
    }

    public Decision check(UUID uuid, String permission) {
        if (permission == null || !permission.startsWith("worldedit.")) return Decision.DEFAULT;
        return allowed(uuid) ? Decision.ALLOW : Decision.DENY;
    }

    public boolean allowed(UUID uuid) {
        if (!authenticated.test(uuid)) { cache.remove(uuid); return false; }
        long now = clock.getAsLong();
        Entry entry = cache.get(uuid);
        if (entry != null && entry.expires() > now) return entry.allowed();
        boolean allowed;
        try { allowed = administrator.test(uuid); }
        catch (RuntimeException unavailable) { allowed = false; }
        cache.put(uuid, new Entry(allowed, now + 2_000_000_000L));
        return allowed;
    }

    public void forget(UUID uuid) { cache.remove(uuid); }
}
