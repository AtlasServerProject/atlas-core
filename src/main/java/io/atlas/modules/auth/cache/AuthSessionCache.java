package io.atlas.modules.auth.cache;

import io.atlas.modules.auth.model.AuthSessionState;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthSessionCache {

    private final Map<UUID, AuthSessionState> sessions = new ConcurrentHashMap<>();

    public void put(AuthSessionState state) {
        sessions.put(state.playerUuid(), state);
    }

    public Optional<AuthSessionState> get(UUID playerUuid) {
        return Optional.ofNullable(sessions.get(playerUuid));
    }

    public void remove(UUID playerUuid) {
        sessions.remove(playerUuid);
    }

    public boolean contains(UUID playerUuid) {
        return sessions.containsKey(playerUuid);
    }

    public Collection<AuthSessionState> getAll() {
        return List.copyOf(sessions.values());
    }

    public void clear() {
        sessions.clear();
    }
}
