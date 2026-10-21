package io.atlas.modules.lobby.service;

import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.auth.service.AuthLobbySpawnService;
import io.atlas.modules.survival.SurvivalModule;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class SpawnTravelService {
    private final AuthLobbySpawnService initialSpawn = new AuthLobbySpawnService();

    public int returnToInitialSpawn(ServerPlayer player) {
        if (!AuthModule.getAuthService().isAuthenticated(player.getUUID())) {
            player.sendSystemMessage(Component.literal("§cVocê precisa se autenticar antes de usar /spawn."));
            return 0;
        }

        player.closeContainer();
        player.stopRiding();
        if (DevEnvironment.DEVELOPMENT) {
            int result = DevTransferService.returnToMain(player);
            if (result == 0) {
                player.sendSystemMessage(Component.literal("§cO endereço do spawn inicial está indisponível. Tente novamente."));
            }
            return result;
        }

        SurvivalModule.getBackService().remember(player);
        initialSpawn.teleportToSpawn(player, player.getServer());
        player.sendSystemMessage(Component.literal("§aVocê voltou ao spawn inicial do Atlas."));
        return 1;
    }
}
