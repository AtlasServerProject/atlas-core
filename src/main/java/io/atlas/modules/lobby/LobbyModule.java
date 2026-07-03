package io.atlas.modules.lobby;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.lobby.listener.LobbyProtectionListener;
import io.atlas.modules.lobby.listener.ServerSelectorListener;
import io.atlas.modules.lobby.service.LobbyProtectionService;
import io.atlas.modules.lobby.service.LobbyPokemonSpawnService;
import io.atlas.modules.lobby.service.LobbyStarterProtectionService;
import io.atlas.modules.lobby.service.ServerSelectorService;
import io.atlas.modules.lobby.service.LobbyTravelService;
import io.atlas.modules.lobby.service.SurvivalWorldService;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rank.RankModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class LobbyModule implements AtlasModule {

    private static final LobbyProtectionService protectionService =
            new LobbyProtectionService(RankModule.getRankService());
    private static final LobbyPokemonSpawnService pokemonSpawnService =
            new LobbyPokemonSpawnService();
    private static final LobbyStarterProtectionService starterProtectionService =
            new LobbyStarterProtectionService();
    private static final LobbyTravelService travelService = new LobbyTravelService();
    private static final ServerSelectorService selectorService = new ServerSelectorService(
            AuthModule.getAuthService(),
            travelService
    );
    private static final SurvivalWorldService survivalWorldService =
            new SurvivalWorldService();

    public static ServerSelectorService getSelectorService() {
        return selectorService;
    }

    @Override
    public String getName() {
        return "Lobby";
    }

    @Override
    public void enable() {
        LobbyProtectionListener.register(protectionService);
        ServerSelectorListener.register(selectorService);
        pokemonSpawnService.enable();
        starterProtectionService.enable();
        ServerTickEvents.END_SERVER_TICK.register(pokemonSpawnService::tick);
        ServerTickEvents.END_SERVER_TICK.register(selectorService::tick);
        ServerLifecycleEvents.SERVER_STARTED.register(survivalWorldService::configure);
        AtlasMod.LOGGER.info("Proteção dos Hubs Atlas iniciada.");
    }

    @Override
    public void disable() {
        protectionService.clear();
        pokemonSpawnService.disable();
        starterProtectionService.disable();
    }
}
