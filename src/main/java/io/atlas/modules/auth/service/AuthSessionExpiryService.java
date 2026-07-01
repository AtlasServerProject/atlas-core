package io.atlas.modules.auth.service;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public class AuthSessionExpiryService {

    private static final int CHECK_INTERVAL_TICKS = 20;

    private final AuthService authService;
    private int ticks;

    public AuthSessionExpiryService(AuthService authService) {
        this.authService = authService;
    }

    public void tick(MinecraftServer server) {
        ticks++;
        if (ticks % CHECK_INTERVAL_TICKS != 0) {
            return;
        }

        authService.expireSessions().stream()
                .filter(AuthService.ExpiredSession::premium)
                .map(session -> server.getPlayerList().getPlayer(session.playerUuid()))
                .filter(player -> player != null)
                .forEach(this::disconnectPremiumPlayer);
    }

    private void disconnectPremiumPlayer(ServerPlayer player) {
        player.connection.disconnect(Component.literal(
                "Sua sessão expirou. Reconecte para autenticar sua conta Premium novamente."
        ));
    }
}
