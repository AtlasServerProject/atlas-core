package io.atlas.modules.lobby.service;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public class LobbyTravelService {

    private static final double EMERALD_X = 918.5302583016859;
    private static final double EMERALD_Y = 71.0;
    private static final double EMERALD_Z = 3838.5058040626927;
    private static final float EMERALD_YAW = 179.39441F;
    private static final float EMERALD_PITCH = -1.7545054F;

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
                EMERALD_PITCH
        );
        return true;
    }
}
