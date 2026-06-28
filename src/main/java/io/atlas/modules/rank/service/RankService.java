package io.atlas.modules.rank.service;

import io.atlas.AtlasMod;
import io.atlas.modules.rank.cache.RankCache;
import io.atlas.modules.rank.model.Rank;
import io.atlas.modules.rank.repository.RankRepository;

import java.util.*;

public class RankService {

    private final RankRepository repository = new RankRepository();
    private final RankCache cache = new RankCache();

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
public boolean hasPermission(UUID uuid, String permission) {


    for (Rank rank : getPlayerRanks(uuid)) {

        Set<String> permissions =
                permissionsByRank.getOrDefault(rank.getId(), Set.of());

        for (String node : permissions) {

            // Permissão total
            if (node.equals("*")) {
                return true;
            }

            // Permissão exata
            if (node.equalsIgnoreCase(permission)) {
                return true;
            }

            // atlas.*
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
}
