package io.atlas.modules.rank.service;

import io.atlas.modules.rank.model.Rank;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class RankSyncService {

    private static final int SYNC_INTERVAL_TICKS = 100;

    private final RankService rankService;
    private final PlayerDisplayService displayService;
    private final Map<UUID, VisualState> visualStates = new HashMap<>();
    private int ticks;

    public RankSyncService(RankService rankService, PlayerDisplayService displayService) {
        this.rankService = rankService;
        this.displayService = displayService;
    }

    public void tick(MinecraftServer server) {
        ticks++;
        if (ticks < SYNC_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;

        Set<UUID> onlinePlayers = new HashSet<>();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();
            onlinePlayers.add(uuid);

            Optional<Rank> highestRank = rankService.getHighestRank(uuid);
            VisualState currentState = VisualState.from(highestRank);
            VisualState previousState = visualStates.put(uuid, currentState);

            if (!currentState.equals(previousState)) {
                displayService.updatePlayer(player, highestRank);
            }
        }

        visualStates.keySet().retainAll(onlinePlayers);
    }

    private record VisualState(long rankId, String prefix, String color, int priority) {

        private static VisualState from(Optional<Rank> rank) {
            return rank
                    .map(value -> new VisualState(
                            value.getId(),
                            value.getPrefix(),
                            value.getColor(),
                            value.getPriority()
                    ))
                    .orElseGet(() -> new VisualState(0, "", "", 0));
        }
    }
}
