package io.atlas.modules.auth.service;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import io.atlas.AtlasMod;
import io.atlas.modules.auth.model.PremiumProfileResolution;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class PremiumProfileResolver {

    private static final Duration PREMIUM_CACHE_DURATION = Duration.ofHours(6);
    private static final Duration OFFLINE_CACHE_DURATION = Duration.ofMinutes(15);

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<PremiumProfileResolution>> pending =
            new ConcurrentHashMap<>();

    public CompletableFuture<PremiumProfileResolution> resolve(
            GameProfileRepository repository,
            String username
    ) {
        String cacheKey = username.toLowerCase(Locale.ROOT);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return CompletableFuture.completedFuture(cached.resolution());
        }

        cache.remove(cacheKey);
        return pending.computeIfAbsent(cacheKey, ignored ->
                CompletableFuture.supplyAsync(() -> lookup(repository, username))
                        .thenApply(resolution -> {
                            cacheResolution(cacheKey, resolution);
                            return resolution;
                        })
                        .whenComplete((resolution, error) -> pending.remove(cacheKey))
        );
    }

    private PremiumProfileResolution lookup(
            GameProfileRepository repository,
            String username
    ) {
        AtomicReference<GameProfile> profile = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();

        repository.findProfilesByNames(new String[]{username}, new ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(GameProfile result) {
                profile.set(result);
            }

            @Override
            public void onProfileLookupFailed(String profileName, Exception exception) {
                failure.set(exception);
            }
        });

        if (profile.get() != null) {
            return PremiumProfileResolution.premium(profile.get());
        }

        if (failure.get() instanceof ProfileNotFoundException) {
            return PremiumProfileResolution.offline();
        }

        AtlasMod.LOGGER.warn(
                "Não foi possível consultar o perfil Premium de {}.",
                username,
                failure.get()
        );
        return PremiumProfileResolution.unavailable();
    }

    private void cacheResolution(String cacheKey, PremiumProfileResolution resolution) {
        Duration duration = switch (resolution.status()) {
            case PREMIUM -> PREMIUM_CACHE_DURATION;
            case OFFLINE -> OFFLINE_CACHE_DURATION;
            case UNAVAILABLE -> Duration.ZERO;
        };

        if (!duration.isZero()) {
            cache.put(cacheKey, new CacheEntry(resolution, Instant.now().plus(duration)));
        }
    }

    private record CacheEntry(
            PremiumProfileResolution resolution,
            Instant expiresAt
    ) {
    }
}
