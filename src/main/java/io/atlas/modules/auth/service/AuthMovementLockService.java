package io.atlas.modules.auth.service;

import io.atlas.modules.auth.model.LockedPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AuthMovementLockService {

    private static final double MAX_DISTANCE_SQUARED = 0.0001;
    private static final int REMINDER_INTERVAL_TICKS = 40;

    private final AuthService authService;
    private final Map<UUID, LockedPosition> lockedPositions = new HashMap<>();
    private int ticks;

    public AuthMovementLockService(AuthService authService) {
        this.authService = authService;
    }

    public void tick(MinecraftServer server) {
        ticks++;
        Set<UUID> onlinePlayers = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            onlinePlayers.add(uuid);

            if (authService.isAuthenticated(uuid)) {
                lockedPositions.remove(uuid);
                continue;
            }

            LockedPosition position = lockedPositions.computeIfAbsent(
                    uuid,
                    ignored -> capture(player)
            );
            enforce(server, player, position);

            if (ticks % REMINDER_INTERVAL_TICKS == 0) {
                sendReminder(player);
            }
        }

        lockedPositions.keySet().retainAll(onlinePlayers);
    }

    public void remove(UUID playerUuid) {
        lockedPositions.remove(playerUuid);
    }

    public void clear() {
        lockedPositions.clear();
    }

    private LockedPosition capture(ServerPlayer player) {
        return new LockedPosition(
                player.serverLevel().dimension(),
                player.getX(),
                player.getY(),
                player.getZ()
        );
    }

    private void enforce(
            MinecraftServer server,
            ServerPlayer player,
            LockedPosition position
    ) {
        player.stopRiding();
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();

        ServerLevel lockedLevel = server.getLevel(position.world());
        if (lockedLevel == null) {
            return;
        }

        boolean changedWorld = player.serverLevel() != lockedLevel;
        double distance = player.position().distanceToSqr(
                position.x(),
                position.y(),
                position.z()
        );

        if (changedWorld || distance > MAX_DISTANCE_SQUARED) {
            player.teleportTo(
                    lockedLevel,
                    position.x(),
                    position.y(),
                    position.z(),
                    player.getYRot(),
                    player.getXRot()
            );
        }
    }

    private void sendReminder(ServerPlayer player) {
        Component message = authService.getSession(player.getUUID())
                .map(state -> {
                    if (state.premium()) {
                        return Component.literal("§eReconecte para autenticar sua conta Premium.");
                    }
                    if (state.registered()) {
                        return Component.literal("§eUse §f/login <senha> §epara liberar seu movimento.");
                    }
                    return Component.literal("§eUse §f/register <senha> <confirmacao> §epara começar.");
                })
                .orElse(Component.literal("§eAguarde o carregamento da sua sessão."));
        player.displayClientMessage(message, true);
    }
}
