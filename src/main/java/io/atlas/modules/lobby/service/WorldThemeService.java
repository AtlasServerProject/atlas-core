package io.atlas.modules.lobby.service;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WorldThemeService {

    private final Map<UUID, ResourceKey<Level>> lastThemeWorlds = new HashMap<>();

    public void playTheme(ServerPlayer player) {
        Level level = player.level();
        ResourceKey<Level> currentWorld = level.dimension();
        ResourceKey<Level> previousWorld = lastThemeWorlds.get(player.getUUID());
        if (currentWorld.equals(previousWorld)) {
            return;
        }
        lastThemeWorlds.put(player.getUUID(), currentWorld);

        if (LobbyWorlds.isEmerald(level)) {
            play(player, SoundEvents.MUSIC_DISC_OTHERSIDE.value(), 0.35F, 1.0F);
            return;
        }
        if (LobbyWorlds.isSurvivalEmerald(level)) {
            play(player, SoundEvents.MUSIC_DISC_FAR.value(), 0.28F, 1.0F);
            return;
        }
        if (LobbyWorlds.isAuth(level)) {
            play(player, SoundEvents.MUSIC_DISC_WAIT.value(), 0.25F, 1.0F);
        }
    }

    private void play(ServerPlayer player, net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        player.level().playSound(null, player.blockPosition(), sound, SoundSource.MASTER, volume, pitch);
    }
}
