package io.atlas.modules.auth.service;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import io.atlas.modules.auth.model.PremiumProfileResolution;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PremiumLoginService {

    private static final PremiumProfileResolver PROFILE_RESOLVER =
            new PremiumProfileResolver();
    private static final Map<UUID, GameProfile> VERIFIED_PROFILES =
            new ConcurrentHashMap<>();

    private PremiumLoginService() {
    }

    public static CompletableFuture<PremiumProfileResolution> resolve(
            GameProfileRepository repository,
            String username
    ) {
        return PROFILE_RESOLVER.resolve(repository, username);
    }

    public static void markVerified(GameProfile profile) {
        VERIFIED_PROFILES.put(profile.getId(), profile);
    }

    public static boolean isVerified(UUID playerUuid) {
        return VERIFIED_PROFILES.containsKey(playerUuid);
    }

    public static void clear(UUID playerUuid) {
        VERIFIED_PROFILES.remove(playerUuid);
    }
}
