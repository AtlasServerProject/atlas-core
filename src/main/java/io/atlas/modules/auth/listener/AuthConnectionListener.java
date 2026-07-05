package io.atlas.modules.auth.listener;

import io.atlas.modules.auth.service.AuthService;
import io.atlas.modules.auth.service.AuthLobbySpawnService;
import io.atlas.modules.auth.service.AuthWelcomeService;
import io.atlas.modules.survival.service.SurvivalPositionService;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;

public class AuthConnectionListener {

    public static void register(
            AuthService authService,
            AuthLobbySpawnService spawnService,
            AuthWelcomeService welcomeService,
            SurvivalPositionService positionService
    ) {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            positionService.saveIfSurvival(handler.player);
            authService.loadPlayer(
                        handler.player.getUUID(),
                        handler.player.getIpAddress()
            );

            spawnService.teleportToSpawn(handler.player, server);

            Component message;
            if (authService.isAuthenticated(handler.player.getUUID())) {
                message = Component.literal("§aConta Premium reconhecida. Login automático concluído.");
            } else if (authService.isRegistered(handler.player.getUUID())) {
                message = Component.literal("§eUse §f/login <senha> §epara autenticar.");
            } else {
                message = Component.literal("§eUse §f/register <senha> <confirmacao> §epara registrar.");
            }
            handler.player.sendSystemMessage(message);
            welcomeService.send(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            positionService.saveIfSurvival(handler.player);
            authService.unloadPlayer(handler.player.getUUID());
        });
    }
}
