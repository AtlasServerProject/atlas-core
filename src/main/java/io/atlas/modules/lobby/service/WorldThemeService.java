package io.atlas.modules.lobby.service;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class WorldThemeService {

    private static final SoundEvent AUTH_THEME = atlasSound("rustboro_city_hoenn2");
    private static final SoundEvent EMERALD_THEME = atlasSound("introductions_hoenn");
    private static final SoundEvent SURVIVAL_ROUTE_THEME = atlasSound("route1_sinnoh");
    private static final SoundEvent SURVIVAL_TOWN_THEME = atlasSound("accumula_town_unova");
    private static final SoundEvent SURVIVAL_ROUTE_ALT_THEME = atlasSound("driftveil_city_unova2");
    private static final SoundEvent SURVIVAL_WATER_THEME = atlasSound("sea_mauville_unova");
    private static final SoundEvent SURVIVAL_SURF_THEME = atlasSound("surfing_hoenn2");
    private static final SoundEvent SURVIVAL_CAVE_THEME = atlasSound("pettleburg_woods-granite_cave");
    private static final SoundEvent END_THEME = atlasSound("distortion_world_sinnoh");

    private final Map<UUID, String> lastThemes = new HashMap<>();
    private final Set<UUID> priorityMusicPlayers = new HashSet<>();

    public void playTheme(ServerPlayer player) {
        if (priorityMusicPlayers.contains(player.getUUID())) {
            return;
        }

        Theme theme = resolveTheme(player);
        String previousTheme = lastThemes.get(player.getUUID());
        if (theme.key().equals(previousTheme)) {
            return;
        }

        lastThemes.put(player.getUUID(), theme.key());
        stopMusic(player);
        play(player, theme.sound(), 1.0F, 1.0F);
    }

    public void playPriority(ServerPlayer player, SoundEvent sound) {
        priorityMusicPlayers.add(player.getUUID());
        stopMusic(player);
        play(player, sound, 1.0F, 1.0F);
    }

    public void clearPriority(ServerPlayer player) {
        if (!priorityMusicPlayers.remove(player.getUUID())) {
            return;
        }
        stopMusic(player);
        lastThemes.remove(player.getUUID());
        playTheme(player);
    }

    public void remove(ServerPlayer player) {
        UUID uuid = player.getUUID();
        priorityMusicPlayers.remove(uuid);
        lastThemes.remove(uuid);
    }

    private Theme resolveTheme(ServerPlayer player) {
        Level level = player.level();
        if (LobbyWorlds.isEmerald(level)) {
            return new Theme("emerald", EMERALD_THEME);
        }
        if (LobbyWorlds.isSurvivalEmerald(level)) {
            return resolveSurvivalTheme(player);
        }
        if (LobbyWorlds.isAuth(level)) {
            return new Theme("auth", AUTH_THEME);
        }
        if (level.dimension().equals(Level.END)) {
            return new Theme("survival:end", END_THEME);
        }

        return new Theme("unknown:" + level.dimension().location(), SURVIVAL_ROUTE_THEME);
    }

    private Theme resolveSurvivalTheme(ServerPlayer player) {
        BlockPos position = player.blockPosition();
        Level level = player.level();

        if (position.getY() < 50 || isCaveBiome(level, position)) {
            return new Theme("survival:cave", SURVIVAL_CAVE_THEME);
        }
        if (player.isInWaterOrBubble() || isOceanOrBeach(level, position)) {
            int waterVariant = Math.floorMod(position.getX() / 256 + position.getZ() / 256, 2);
            return waterVariant == 0
                    ? new Theme("survival:water:sea", SURVIVAL_WATER_THEME)
                    : new Theme("survival:water:surf", SURVIVAL_SURF_THEME);
        }

        int regionX = Math.floorDiv(position.getX(), 256);
        int regionZ = Math.floorDiv(position.getZ(), 256);
        int variant = Math.floorMod(regionX * 31 + regionZ * 17, 3);
        return switch (variant) {
            case 1 -> new Theme("survival:route:" + regionX + ":" + regionZ + ":town", SURVIVAL_TOWN_THEME);
            case 2 -> new Theme("survival:route:" + regionX + ":" + regionZ + ":alt", SURVIVAL_ROUTE_ALT_THEME);
            default -> new Theme("survival:route:" + regionX + ":" + regionZ + ":base", SURVIVAL_ROUTE_THEME);
        };
    }

    private boolean isCaveBiome(Level level, BlockPos position) {
        Optional<ResourceKey<Biome>> biome = level.getBiome(position).unwrapKey();
        return biome
                .map(key -> key.equals(Biomes.DRIPSTONE_CAVES)
                        || key.equals(Biomes.LUSH_CAVES)
                        || key.equals(Biomes.DEEP_DARK))
                .orElse(false);
    }

    private boolean isOceanOrBeach(Level level, BlockPos position) {
        Optional<ResourceKey<Biome>> biome = level.getBiome(position).unwrapKey();
        return biome
                .map(key -> key.equals(Biomes.BEACH)
                        || key.equals(Biomes.SNOWY_BEACH)
                        || key.equals(Biomes.OCEAN)
                        || key.equals(Biomes.DEEP_OCEAN)
                        || key.equals(Biomes.COLD_OCEAN)
                        || key.equals(Biomes.DEEP_COLD_OCEAN)
                        || key.equals(Biomes.FROZEN_OCEAN)
                        || key.equals(Biomes.DEEP_FROZEN_OCEAN)
                        || key.equals(Biomes.WARM_OCEAN)
                        || key.equals(Biomes.LUKEWARM_OCEAN)
                        || key.equals(Biomes.DEEP_LUKEWARM_OCEAN))
                .orElse(false);
    }

    private void play(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.playNotifySound(sound, SoundSource.MUSIC, volume, pitch);
    }

    private void stopMusic(ServerPlayer player) {
        player.connection.send(new ClientboundStopSoundPacket(null, SoundSource.MUSIC));
    }

    private static SoundEvent atlasSound(String path) {
        return SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath("cobblesounds", path)
        );
    }

    private record Theme(String key, SoundEvent sound) {
    }
}
