package io.atlas.modules.auth.service;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class AuthLobbySpawnService {

    private static final int JOIN_TELEPORT_DELAY_TICKS = 1;
    private static final double SAME_POSITION_DISTANCE_SQUARED = 0.25;
    private static final double SPAWN_X = 642.215;
    private static final double SPAWN_Y = 126.0;
    private static final double SPAWN_Z = 3534.426;
    private static final float SPAWN_YAW = 91.8F;
    private static final float SPAWN_PITCH = -3.4F;

    private final Map<UUID, Integer> pendingTeleports = new HashMap<>();

    public void scheduleTeleportToSpawn(ServerPlayer player) {
        pendingTeleports.put(player.getUUID(), JOIN_TELEPORT_DELAY_TICKS);
    }

    public void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, Integer>> iterator = pendingTeleports.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int remainingTicks = entry.getValue() - 1;
            if (remainingTicks > 0) {
                entry.setValue(remainingTicks);
                continue;
            }

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                teleportToSpawn(player, server);
            }
            iterator.remove();
        }
    }

    public void remove(UUID playerUuid) {
        pendingTeleports.remove(playerUuid);
    }

    public void clear() {
        pendingTeleports.clear();
    }

    public void teleportToSpawn(ServerPlayer player, MinecraftServer server) {
        ServerLevel authLobby = server.overworld();
        player.stopRiding();
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();

        if (isAlreadyAtSpawn(player, authLobby)) {
            return;
        }

        player.teleportTo(
                authLobby,
                SPAWN_X,
                SPAWN_Y,
                SPAWN_Z,
                SPAWN_YAW,
                SPAWN_PITCH
        );
    }

    private boolean isAlreadyAtSpawn(ServerPlayer player, ServerLevel authLobby) {
        return player.serverLevel() == authLobby
                && player.position().distanceToSqr(SPAWN_X, SPAWN_Y, SPAWN_Z)
                <= SAME_POSITION_DISTANCE_SQUARED;
    }
}
