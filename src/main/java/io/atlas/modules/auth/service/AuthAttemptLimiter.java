package io.atlas.modules.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class AuthAttemptLimiter {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(5);

    private final Map<UUID, AttemptState> attempts = new HashMap<>();

    public synchronized Optional<Duration> remainingCooldown(UUID playerUuid) {
        AttemptState state = attempts.get(playerUuid);
        if (state == null || state.blockedUntil() == null) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        if (!state.blockedUntil().isAfter(now)) {
            attempts.remove(playerUuid);
            return Optional.empty();
        }

        return Optional.of(Duration.between(now, state.blockedUntil()));
    }

    public synchronized boolean recordFailure(UUID playerUuid) {
        AttemptState current = attempts.getOrDefault(
                playerUuid,
                new AttemptState(0, null)
        );
        int failures = current.failures() + 1;

        if (failures >= MAX_FAILED_ATTEMPTS) {
            attempts.put(
                    playerUuid,
                    new AttemptState(failures, Instant.now().plus(LOCK_DURATION))
            );
            return true;
        }

        attempts.put(playerUuid, new AttemptState(failures, null));
        return false;
    }

    public synchronized void reset(UUID playerUuid) {
        attempts.remove(playerUuid);
    }

    public synchronized void clear() {
        attempts.clear();
    }

    private record AttemptState(int failures, Instant blockedUntil) {
    }
}
