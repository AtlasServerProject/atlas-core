package io.atlas.modules.survival.listener;

import io.atlas.modules.lobby.service.LobbyTravelService;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.survival.service.SurvivalPositionService;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public final class SurvivalRespawnListener {

    private SurvivalRespawnListener() {
    }

    public static void register(
            SurvivalPositionService positionService,
            LobbyTravelService travelService
    ) {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive || !LobbyWorlds.isSurvivalEmerald(oldPlayer.level())) {
                return;
            }
            positionService.clear(newPlayer);
            travelService.teleportToEmerald(newPlayer);
            newPlayer.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§eVocê morreu no Survival e retornou ao Lobby Emerald."
                    ),
                    false
            );
        });
    }
}
