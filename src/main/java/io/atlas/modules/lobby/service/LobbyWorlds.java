package io.atlas.modules.lobby.service;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class LobbyWorlds {

    public static final ResourceKey<Level> EMERALD = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("atlas", "emerald")
    );

    private LobbyWorlds() {
    }

    public static boolean isAuth(Level level) {
        return level.dimension() == Level.OVERWORLD;
    }

    public static boolean isEmerald(Level level) {
        return level.dimension().equals(EMERALD);
    }

    public static boolean isLobby(Level level) {
        return isAuth(level) || isEmerald(level);
    }
}
