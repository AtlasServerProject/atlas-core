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
            io.atlas.modules.auth.AuthModule.getReconnectPositions().joined(handler.player);
            positionService.saveIfSurvival(handler.player);
            authService.loadPlayer(
                        handler.player.getUUID(),
                        handler.player.getIpAddress()
            );

            spawnService.scheduleTeleportToSpawn(handler.player);

            Component message;
            if (io.atlas.modules.lobby.service.DevEnvironment.DEVELOPMENT) {
                message = Component.literal("§6Lobby de testes. §7Inventário, Pokémon e saldo separados do Emerald. §e/lobby emerald §7para voltar.");
                io.atlas.modules.rank.RankModule.getDisplayService().updatePlayer(handler.player);
            } else if (authService.isAuthenticated(handler.player.getUUID())) {
                message = Component.literal("§aConta Premium reconhecida. Login automático concluído.");
            } else if (authService.isRegistered(handler.player.getUUID())) {
                message = Component.literal("§eUse §f/login <senha> §epara autenticar.");
            } else {
                message = Component.literal("§eUse §f/register <senha> <confirmacao> [email] §epara registrar.");
            }
            handler.player.sendSystemMessage(message);
            welcomeService.send(handler.player);
            io.atlas.modules.auth.AuthModule.getRecoveryService().notifyMissingEmail(handler.player);
            server.getCommands().sendCommands(handler.player);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            io.atlas.modules.auth.AuthModule.getReconnectPositions().disconnected(handler.player);
            positionService.saveIfSurvival(handler.player);
            spawnService.remove(handler.player.getUUID());
            authService.unloadPlayer(handler.player.getUUID());
            io.atlas.modules.lobby.service.DevTransferService.forget(handler.player);
            io.atlas.modules.lobby.service.DevLobbyAccessService.forget(handler.player.getUUID());
        });
    }
}
