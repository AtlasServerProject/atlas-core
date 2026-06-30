package io.atlas.modules.auth.model;

import com.mojang.authlib.GameProfile;

public record PremiumProfileResolution(Status status, GameProfile profile) {

    public static PremiumProfileResolution premium(GameProfile profile) {
        return new PremiumProfileResolution(Status.PREMIUM, profile);
    }

    public static PremiumProfileResolution offline() {
        return new PremiumProfileResolution(Status.OFFLINE, null);
    }

    public static PremiumProfileResolution unavailable() {
        return new PremiumProfileResolution(Status.UNAVAILABLE, null);
    }

    public enum Status {
        PREMIUM,
        OFFLINE,
        UNAVAILABLE
    }
}
