package io.atlas.modules.lobby;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.lobby.listener.LobbyProtectionListener;
import io.atlas.modules.lobby.service.LobbyProtectionService;
import io.atlas.modules.lobby.service.LobbyPokemonSpawnService;
import io.atlas.modules.rank.RankModule;

public class LobbyModule implements AtlasModule {

    private static final LobbyProtectionService protectionService =
            new LobbyProtectionService(RankModule.getRankService());
    private static final LobbyPokemonSpawnService pokemonSpawnService =
            new LobbyPokemonSpawnService();

    @Override
    public String getName() {
        return "Lobby";
    }

    @Override
    public void enable() {
        LobbyProtectionListener.register(protectionService);
        pokemonSpawnService.enable();
        AtlasMod.LOGGER.info("Proteção do Auth Lobby iniciada.");
    }

    @Override
    public void disable() {
        protectionService.clear();
        pokemonSpawnService.disable();
    }
}
