package io.atlas.modules.rank.service;

import io.atlas.AtlasMod;
import io.atlas.modules.rank.cache.RankCache;
import io.atlas.modules.rank.formatter.RankFormatter;
import io.atlas.modules.rank.model.Rank;
import io.atlas.modules.rank.repository.RankRepository;
import net.minecraft.network.chat.Component;

import java.util.*;

public class RankService {

    private static final Set<String> RANK_MANAGERS = Set.of(
            "OWNER", "DONO", "ADMIN", "ADM"
    );

    private final RankRepository repository = new RankRepository();
    private final RankCache cache = new RankCache();
    private final RankFormatter formatter = new RankFormatter();

    private final Map<Long, Set<String>> permissionsByRank = new HashMap<>();

    public void loadRanks() {
        cache.clear();
        permissionsByRank.clear();

        for (Rank rank : repository.findAllRanks()) {
            cache.put(rank);
            permissionsByRank.put(rank.getId(), repository.findPermissionsByRankId(rank.getId()));
        }

        AtlasMod.LOGGER.info("{} ranks carregados.", cache.getAll().size());
    }

    public List<Rank> getPlayerRanks(UUID uuid) {
        return repository.findRanksByPlayerUuid(uuid);
    }

    public Optional<Rank> getHighestRank(UUID uuid) {
        return getPlayerRanks(uuid).stream()
                .max(Comparator.comparingInt(Rank::getPriority));
    }

    public Component formatPlayerName(UUID uuid, String username) {
        return getHighestRank(uuid)
                .map(rank -> formatter.formatPlayerName(rank, username))
                .orElseGet(() -> formatter.formatPlayerName(username));
    }

    public boolean canManageRanks(UUID uuid) {
        return getPlayerRanks(uuid).stream()
                .map(Rank::getIdentifier)
                .map(identifier -> identifier.toUpperCase(Locale.ROOT))
                .anyMatch(RANK_MANAGERS::contains);
    }

    public RankAssignmentResult assignRank(String username, String rankIdentifier) {
        Optional<Rank> rank = cache.getByIdentifier(rankIdentifier);
        if (rank.isEmpty()) {
            return RankAssignmentResult.RANK_NOT_FOUND;
        }

        Optional<UUID> playerUuid = repository.findPlayerUuidByUsername(username);
        if (playerUuid.isEmpty()) {
            return RankAssignmentResult.PLAYER_NOT_FOUND;
        }

        repository.assignRank(playerUuid.get(), rank.get().getId());
        return RankAssignmentResult.SUCCESS;
    }

    public boolean hasPermission(UUID uuid, String permission) {
        for (Rank rank : getPlayerRanks(uuid)) {
            Set<String> permissions =
                    permissionsByRank.getOrDefault(rank.getId(), Set.of());

            for (String node : permissions) {
                if (node.equals("*")) {
                    return true;
                }

                if (node.equalsIgnoreCase(permission)) {
                    return true;
                }

                if (node.endsWith("*")) {
                    String prefix = node.substring(0, node.length() - 1);

                    if (permission.startsWith(prefix)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isStaff(UUID uuid) {
        return getPlayerRanks(uuid).stream().anyMatch(Rank::isStaff);
    }

    public enum RankAssignmentResult {
        SUCCESS,
        PLAYER_NOT_FOUND,
        RANK_NOT_FOUND
    }
}
