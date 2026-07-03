package io.atlas.modules.lobby.service;

import io.atlas.modules.rank.service.RankService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LobbyProtectionService {

    private static final Duration MESSAGE_COOLDOWN = Duration.ofSeconds(2);

    private final RankService rankService;
    private final Map<UUID, Instant> lastDenialMessages = new HashMap<>();

    public LobbyProtectionService(RankService rankService) {
        this.rankService = rankService;
    }

    public boolean canModify(Player player, Level world) {
        if (!LobbyWorlds.isLobby(world) || rankService.canManageRanks(player.getUUID())) {
            return true;
        }

        sendDenialMessage(player);
        return false;
    }

    public void clear() {
        lastDenialMessages.clear();
    }

    private void sendDenialMessage(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Instant now = Instant.now();
        Instant lastMessage = lastDenialMessages.get(player.getUUID());
        if (lastMessage != null && lastMessage.plus(MESSAGE_COOLDOWN).isAfter(now)) {
            return;
        }

        lastDenialMessages.put(player.getUUID(), now);
        serverPlayer.displayClientMessage(
                Component.literal("§cVocê não pode construir no Hub."),
                true
        );
    }
}
