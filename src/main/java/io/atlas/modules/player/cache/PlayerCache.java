package io.atlas.modules.player.cache;

import io.atlas.modules.player.model.PlayerProfile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerCache {

    private final Map<UUID, PlayerProfile> profiles = new ConcurrentHashMap<>();

    public void put(PlayerProfile profile) {
        profiles.put(profile.getUuid(), profile);
    }

    public Optional<PlayerProfile> get(UUID uuid) {
        return Optional.ofNullable(profiles.get(uuid));
    }

    public void remove(UUID uuid) {
        profiles.remove(uuid);
    }

    public boolean contains(UUID uuid) {
        return profiles.containsKey(uuid);
    }
}