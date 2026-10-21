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

    public boolean loadPlayer(UUID uuid, String username) {

        if (PremiumLoginService.isVerified(uuid)) {
            repository.promotePremiumIdentity(uuid, username);
        }

        Optional<PlayerProfile> optional = repository.findByUuid(uuid);

        boolean firstJoin = optional.isEmpty();
        if (firstJoin) {

            AtlasMod.LOGGER.info("Novo jogador detectado: {}", username);

            repository.create(uuid, username);

            optional = repository.findByUuid(uuid);
        }

        PlayerProfile profile = optional.get();

        repository.updateLogin(uuid, username);

        cache.put(profile);

        AtlasMod.LOGGER.info("Player {} carregado para o cache.", username);
        return firstJoin;
    }

    public void welcome(net.minecraft.server.level.ServerPlayer player) {
        boolean first = loadPlayer(player.getUUID(), player.getName().getString());
        io.atlas.modules.rank.RankModule.getDisplayService().updatePlayer(player);
        var message = net.minecraft.network.chat.Component.literal("✦ ").withStyle(net.minecraft.ChatFormatting.GOLD)
            .append(player.getDisplayName())
            .append(net.minecraft.network.chat.Component.literal(first
                ? " chegou ao Atlas pela primeira vez. Boas-vindas à sua nova aventura!"
                : " voltou ao Atlas. Sua jornada continua!").withStyle(net.minecraft.ChatFormatting.AQUA));
        player.getServer().getPlayerList().broadcastSystemMessage(message, false);
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
