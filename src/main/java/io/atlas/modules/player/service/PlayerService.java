package io.atlas.modules.player.service;

import io.atlas.AtlasMod;
import io.atlas.modules.player.cache.PlayerCache;
import io.atlas.modules.player.model.PlayerProfile;
import io.atlas.modules.player.repository.PlayerRepository;
import io.atlas.modules.auth.service.PremiumLoginService;

import java.util.Optional;
import java.util.UUID;

public class PlayerService {

    private final PlayerRepository repository = new PlayerRepository();
    private final PlayerCache cache = new PlayerCache();

    public void loadPlayer(UUID uuid, String username) {

        if (PremiumLoginService.isVerified(uuid)) {
            repository.promotePremiumIdentity(uuid, username);
        }

        Optional<PlayerProfile> optional = repository.findByUuid(uuid);

        if (optional.isEmpty()) {

            AtlasMod.LOGGER.info("Novo jogador detectado: {}", username);

            repository.create(uuid, username);

            optional = repository.findByUuid(uuid);
        }

        PlayerProfile profile = optional.get();

        repository.updateLogin(uuid, username);

        cache.put(profile);

        AtlasMod.LOGGER.info("Player {} carregado para o cache.", username);
    }

    public void unloadPlayer(UUID uuid) {

        repository.updateLogout(uuid);

        cache.remove(uuid);

        AtlasMod.LOGGER.info("Player {} removido do cache.", uuid);
    }

    public Optional<PlayerProfile> getProfile(UUID uuid) {
        return cache.get(uuid);
    }

    public boolean isLoaded(UUID uuid) {
        return cache.contains(uuid);
    }
}
