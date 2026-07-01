package io.atlas.modules.auth.service;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

public class AuthWelcomeService {

    private static final int FADE_IN_TICKS = 10;
    private static final int STAY_TICKS = 80;
    private static final int FADE_OUT_TICKS = 20;

    private final AuthService authService;

    public AuthWelcomeService(AuthService authService) {
        this.authService = authService;
    }

    public void send(ServerPlayer player) {
        Component title = Component.literal("Seja bem-vindo ao Atlas Cobblemon")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        Component subtitle = subtitleFor(player);

        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                FADE_IN_TICKS,
                STAY_TICKS,
                FADE_OUT_TICKS
        ));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }

    private Component subtitleFor(ServerPlayer player) {
        if (authService.isAuthenticated(player.getUUID())) {
            return Component.literal("Conta Premium autenticada")
                    .withStyle(ChatFormatting.GOLD);
        }
        if (authService.isRegistered(player.getUUID())) {
            return Component.literal("Use /login <senha> para continuar")
                    .withStyle(ChatFormatting.GOLD);
        }
        return Component.literal("Use /register <senha> <confirmacao> para começar")
                .withStyle(ChatFormatting.GOLD);
    }
}
