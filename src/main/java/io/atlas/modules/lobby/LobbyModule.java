package io.atlas.modules.lobby;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.lobby.listener.LobbyProtectionListener;
import io.atlas.modules.lobby.service.LobbyProtectionService;
import io.atlas.modules.lobby.service.LobbyPokemonSpawnService;
import io.atlas.modules.lobby.service.LobbyStarterProtectionService;
import io.atlas.modules.rank.RankModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class LobbyModule implements AtlasModule {

    private static final LobbyProtectionService protectionService =
            new LobbyProtectionService(RankModule.getRankService());
    private static final LobbyPokemonSpawnService pokemonSpawnService =
            new LobbyPokemonSpawnService();
    private static final LobbyStarterProtectionService starterProtectionService =
            new LobbyStarterProtectionService();

    @Override
    public String getName() {
        return "Lobby";
    }

    @Override
    public void enable() {
        LobbyProtectionListener.register(protectionService);
        pokemonSpawnService.enable();
        starterProtectionService.enable();
        ServerTickEvents.END_SERVER_TICK.register(pokemonSpawnService::tick);
        AtlasMod.LOGGER.info("Proteção do Auth Lobby iniciada.");
    }

    @Override
    public void disable() {
        protectionService.clear();
        pokemonSpawnService.disable();
        starterProtectionService.disable();
    }
}
