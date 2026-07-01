package io.atlas.modules.auth.service;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class AuthLobbySpawnService {

    private static final double SPAWN_X = 646.5;
    private static final double SPAWN_Y = 83.0;
    private static final double SPAWN_Z = 3535.5;
    private static final float SPAWN_YAW = 90.0F;
    private static final float SPAWN_PITCH = 0.0F;

    public void teleportToSpawn(ServerPlayer player, MinecraftServer server) {
        ServerLevel authLobby = server.overworld();
        player.teleportTo(
                authLobby,
                SPAWN_X,
                SPAWN_Y,
                SPAWN_Z,
                SPAWN_YAW,
                SPAWN_PITCH
        );
    }
}
