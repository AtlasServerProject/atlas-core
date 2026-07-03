package io.atlas.modules.survival;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.rank.RankModule;
import io.atlas.modules.survival.service.RandomTeleportService;
import io.atlas.modules.survival.service.SurvivalPositionService;
import io.atlas.modules.survival.listener.SurvivalRespawnListener;
import io.atlas.modules.lobby.service.LobbyTravelService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class SurvivalModule implements AtlasModule {

    private static final RandomTeleportService RANDOM_TELEPORT_SERVICE =
            new RandomTeleportService(RankModule.getRankService());
    private static final SurvivalPositionService SURVIVAL_POSITION_SERVICE =
            new SurvivalPositionService();

    public static RandomTeleportService getRandomTeleportService() {
        return RANDOM_TELEPORT_SERVICE;
    }

    public static SurvivalPositionService getPositionService() {
        return SURVIVAL_POSITION_SERVICE;
    }

    @Override
    public String getName() {
        return "Survival";
    }

    @Override
    public void enable() {
        ServerTickEvents.END_SERVER_TICK.register(RANDOM_TELEPORT_SERVICE::tick);
        ServerTickEvents.END_SERVER_TICK.register(SURVIVAL_POSITION_SERVICE::tick);
        SurvivalRespawnListener.register(SURVIVAL_POSITION_SERVICE, new LobbyTravelService());
        AtlasMod.LOGGER.info("Serviços do Survival Emerald iniciados.");
    }

    @Override
    public void disable() {
        RANDOM_TELEPORT_SERVICE.clear();
    }
}
