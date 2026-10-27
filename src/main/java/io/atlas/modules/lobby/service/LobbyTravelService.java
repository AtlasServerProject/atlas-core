package io.atlas.modules.lobby.service;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class LobbyTravelService {

    private static final double EMERALD_X = 918.526;
    private static final double EMERALD_Y = 71.0;
    private static final double EMERALD_Z = 3808.433;
    private static final float EMERALD_YAW = 179.6F;
    private static final float EMERALD_PITCH = 6.5F;

    public boolean teleportToEmerald(ServerPlayer player) {
        ServerLevel emerald = player.getServer().getLevel(LobbyWorlds.EMERALD);
        if (emerald == null) {
            player.displayClientMessage(
                    Component.literal("§cO Lobby Emerald está temporariamente indisponível."),
                    false
            );
            return false;
        }

        player.closeContainer();
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(
                emerald,
                DevEnvironment.DEVELOPMENT ? 0.5 : EMERALD_X,
                DevEnvironment.DEVELOPMENT ? 64.0 : EMERALD_Y,
                DevEnvironment.DEVELOPMENT ? 0.5 : EMERALD_Z,
                EMERALD_YAW,
                EMERALD_PITCH
        );
        return true;
    }
}
