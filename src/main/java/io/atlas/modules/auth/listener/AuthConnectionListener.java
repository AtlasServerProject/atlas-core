package io.atlas.modules.auth.listener;

import io.atlas.modules.auth.service.AuthService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;

public class AuthConnectionListener {

    public static void register(AuthService authService) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            authService.loadPlayer(
                        handler.player.getUUID(),
                        handler.player.getIpAddress()
            );

            Component message;
            if (authService.isAuthenticated(handler.player.getUUID())) {
                message = Component.literal("§aConta Premium reconhecida. Login automático concluído.");
            } else if (authService.isRegistered(handler.player.getUUID())) {
                message = Component.literal("§eUse §f/login <senha> §epara autenticar.");
            } else {
                message = Component.literal("§eUse §f/register <senha> <confirmacao> §epara registrar.");
            }
            handler.player.sendSystemMessage(message);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                authService.unloadPlayer(handler.player.getUUID())
        );
    }
}
