package io.atlas.modules.survival.service;

import io.atlas.modules.lobby.service.LobbyTravelService;
import io.atlas.modules.lobby.service.LobbyWorlds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class SurvivalRulesService {

    private static final int CHECK_INTERVAL_TICKS = 10;
    private static final int VOID_MARGIN_BLOCKS = 16;

    private final LobbyTravelService lobbyTravelService = new LobbyTravelService();
    private int ticks;

    public void tick(MinecraftServer server) {
        ticks++;
        if (ticks < CHECK_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isManagedWorld(player)) {
                continue;
            }

            keepFed(player);
            protectFromVoid(player);
        }
    }

    private boolean isManagedWorld(ServerPlayer player) {
        return LobbyWorlds.isLobby(player.level()) || LobbyWorlds.isSurvivalArea(player.level());
    }

    private void keepFed(ServerPlayer player) {
        var food = player.getFoodData();
        if (food.getFoodLevel() < 20) {
            food.setFoodLevel(20);
        }
        if (food.getSaturationLevel() < 5.0F) {
            food.setSaturation(5.0F);
        }
        if (food.getExhaustionLevel() > 0.0F) {
            food.setExhaustion(0.0F);
        }
    }

    private void protectFromVoid(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        int voidY = level.getMinBuildHeight() - VOID_MARGIN_BLOCKS;
        if (player.getY() > voidY) {
            return;
        }

        player.setHealth(player.getMaxHealth());
        player.resetFallDistance();

        if (LobbyWorlds.isSurvivalArea(level)) {
            if (lobbyTravelService.teleportToEmerald(player)) {
                player.displayClientMessage(
                        Component.literal("§eVocê caiu no Void e foi enviado ao Lobby Emerald."),
                        false
                );
            }
            return;
        }

        player.teleportTo(
                level,
                level.getSharedSpawnPos().getX() + 0.5,
                level.getSharedSpawnPos().getY(),
                level.getSharedSpawnPos().getZ() + 0.5,
                level.getSharedSpawnAngle(),
                player.getXRot()
        );
        player.displayClientMessage(
                Component.literal("§eVocê caiu no Void e voltou ao spawn."),
                false
        );
    }
}
