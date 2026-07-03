package io.atlas.modules.lobby.service;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class LobbyTravelService {

    private static final double EMERALD_X = 975.5;
    private static final double EMERALD_Y = 179.0;
    private static final double EMERALD_Z = 1573.5;
    private static final float EMERALD_YAW = -90.0F;

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
                EMERALD_X,
                EMERALD_Y,
                EMERALD_Z,
                EMERALD_YAW,
                0.0F
        );
        return true;
    }
}
