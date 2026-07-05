package io.atlas.modules.auth.service;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public class AuthLobbySpawnService {

    private static final double SPAWN_X = 642.215;
    private static final double SPAWN_Y = 126.0;
    private static final double SPAWN_Z = 3534.426;
    private static final float SPAWN_YAW = 91.8F;
    private static final float SPAWN_PITCH = -3.4F;

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
