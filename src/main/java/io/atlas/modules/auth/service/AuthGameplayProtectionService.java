package io.atlas.modules.auth.service;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthGameplayProtectionService {

    private static final Duration MESSAGE_COOLDOWN = Duration.ofSeconds(2);

    private final AuthService authService;
    private final Map<UUID, Instant> lastMessages = new HashMap<>();

    public AuthGameplayProtectionService(AuthService authService) {
        this.authService = authService;
    }

    public boolean canInteract(Player player) {
        if (authService.isAuthenticated(player.getUUID())) {
            return true;
        }

        sendLoginRequiredMessage(player);
        return false;
    }

    public boolean canUseInventory(ServerPlayer player) {
        return canInteract(player);
    }

    public void clear() {
        lastMessages.clear();
    }

    private void sendLoginRequiredMessage(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        Instant now = Instant.now();
        Instant lastMessage = lastMessages.get(player.getUUID());
        if (lastMessage != null && lastMessage.plus(MESSAGE_COOLDOWN).isAfter(now)) {
            return;
        }

        lastMessages.put(player.getUUID(), now);
        serverPlayer.displayClientMessage(
                Component.literal("§cFaça login para usar inventário ou interagir."),
                true
        );
    }
}
