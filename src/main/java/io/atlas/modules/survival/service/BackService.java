package io.atlas.modules.survival.service;

import io.atlas.modules.lobby.service.LobbyWorlds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BackService {

    private final Map<UUID, LastLocation> locations = new ConcurrentHashMap<>();

    public void remember(ServerPlayer player) {
        if (player == null || !canRemember(player)) {
            return;
        }

        locations.put(player.getUUID(), LastLocation.from(player));
    }

    public boolean back(ServerPlayer player) {
        LastLocation location = locations.get(player.getUUID());
        if (location == null) {
            player.displayClientMessage(
                    Component.literal("§eNenhuma localização anterior foi salva ainda."),
                    false
            );
            return false;
        }

        ServerLevel level = player.getServer().getLevel(location.level());
        if (level == null) {
            player.displayClientMessage(
                    Component.literal("§cO mundo da sua última localização não está disponível agora."),
                    false
            );
            return false;
        }

        LastLocation current = canRemember(player) ? LastLocation.from(player) : null;
        player.stopRiding();
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(
                level,
                location.x(),
                location.y(),
                location.z(),
                location.yaw(),
                location.pitch()
        );
        if (current != null) {
            locations.put(player.getUUID(), current);
        } else {
            locations.remove(player.getUUID());
        }
        player.displayClientMessage(
                Component.literal("§aVocê voltou para sua última localização."),
                false
        );
        return true;
    }

    public void clear(ServerPlayer player) {
        if (player != null) {
            locations.remove(player.getUUID());
        }
    }

    public void clear() {
        locations.clear();
    }

    private boolean canRemember(ServerPlayer player) {
        Level level = player.level();
        return !LobbyWorlds.isAuth(level);
    }

    private record LastLocation(
            ResourceKey<Level> level,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        private static LastLocation from(ServerPlayer player) {
            return new LastLocation(
                    player.level().dimension(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    player.getYRot(),
                    player.getXRot()
            );
        }
    }
}
