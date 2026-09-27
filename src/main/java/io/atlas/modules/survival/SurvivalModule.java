package io.atlas.modules.survival;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.rank.RankModule;
import io.atlas.modules.survival.service.RandomTeleportService;
import io.atlas.modules.survival.service.SurvivalRulesService;
import io.atlas.modules.survival.service.SurvivalPositionService;
import io.atlas.modules.survival.service.BackService;
import io.atlas.modules.survival.listener.SurvivalRespawnListener;
import io.atlas.modules.lobby.service.LobbyTravelService;
import io.atlas.modules.lobby.service.WorldThemeService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class SurvivalModule implements AtlasModule {

    private static final RandomTeleportService RANDOM_TELEPORT_SERVICE =
            new RandomTeleportService(RankModule.getRankService());
    private static final SurvivalPositionService SURVIVAL_POSITION_SERVICE =
            new SurvivalPositionService();
    private static final SurvivalRulesService SURVIVAL_RULES_SERVICE =
            new SurvivalRulesService();
    private static final WorldThemeService WORLD_THEME_SERVICE = new WorldThemeService();
    private static final BackService BACK_SERVICE = new BackService();

    public static RandomTeleportService getRandomTeleportService() {
        return RANDOM_TELEPORT_SERVICE;
    }

    public static SurvivalPositionService getPositionService() {
        return SURVIVAL_POSITION_SERVICE;
    }

    public static WorldThemeService getWorldThemeService() {
        return WORLD_THEME_SERVICE;
    }

    public static BackService getBackService() {
        return BACK_SERVICE;
    }

    @Override
    public String getName() {
        return "Survival";
    }

    @Override
    public void enable() {
        ServerTickEvents.END_SERVER_TICK.register(RANDOM_TELEPORT_SERVICE::tick);
        ServerTickEvents.END_SERVER_TICK.register(SURVIVAL_POSITION_SERVICE::tick);
        ServerTickEvents.END_SERVER_TICK.register(SURVIVAL_RULES_SERVICE::tick);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (var player : server.getPlayerList().getPlayers()) {
                WORLD_THEME_SERVICE.playTheme(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                WORLD_THEME_SERVICE.remove(handler.player)
        );
        SurvivalRespawnListener.register(SURVIVAL_POSITION_SERVICE, new LobbyTravelService());
        AtlasMod.LOGGER.info("Serviços do Survival Emerald iniciados.");
    }

    @Override
    public void disable() {
        RANDOM_TELEPORT_SERVICE.clear();
        BACK_SERVICE.clear();
    }
}
