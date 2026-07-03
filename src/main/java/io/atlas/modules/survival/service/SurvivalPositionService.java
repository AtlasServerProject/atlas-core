package io.atlas.modules.survival.service;

import io.atlas.AtlasMod;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.survival.model.SurvivalPosition;
import io.atlas.modules.survival.repository.SurvivalPositionRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class SurvivalPositionService {

    private static final int SAVE_INTERVAL_TICKS = 600;
    private final SurvivalPositionRepository repository = new SurvivalPositionRepository();
    private int ticks;

    public void tick(MinecraftServer server) {
        ticks++;
        if (ticks < SAVE_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            saveIfSurvival(player);
        }
    }

    public void saveIfSurvival(ServerPlayer player) {
        if (!LobbyWorlds.isSurvivalEmerald(player.level())) {
            return;
        }
        repository.save(player.getUUID(), currentPosition(player));
    }

    public boolean restore(ServerPlayer player) {
        ServerLevel survival = player.getServer().getLevel(LobbyWorlds.SURVIVAL_EMERALD);
        if (survival == null) {
            return false;
        }
        return repository.find(player.getUUID())
                .map(position -> {
                    player.setDeltaMovement(Vec3.ZERO);
                    player.teleportTo(
                            survival,
                            position.x(),
                            position.y(),
                            position.z(),
                            position.yaw(),
                            position.pitch()
                    );
                    player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§aVocê retornou à sua última posição no Survival Emerald."
                            ),
                            false
                    );
                    return true;
                })
                .orElse(false);
    }

    public void clear(ServerPlayer player) {
        repository.delete(player.getUUID());
    }

    private SurvivalPosition currentPosition(ServerPlayer player) {
        return new SurvivalPosition(
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot()
        );
    }
}
