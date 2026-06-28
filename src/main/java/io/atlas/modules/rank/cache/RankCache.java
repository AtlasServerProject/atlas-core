package io.atlas.modules.rank.cache;

import io.atlas.modules.rank.model.Rank;

import java.util.*;

public class RankCache {

    private final Map<String, Rank> ranksByIdentifier = new HashMap<>();
    private final Map<Long, Rank> ranksById = new HashMap<>();

    public void put(Rank rank) {
        ranksByIdentifier.put(rank.getIdentifier(), rank);
        ranksById.put(rank.getId(), rank);
    }

    public Optional<Rank> getByIdentifier(String identifier) {
        return Optional.ofNullable(ranksByIdentifier.get(identifier));
    }

    public Optional<Rank> getById(long id) {
        return Optional.ofNullable(ranksById.get(id));
    }

    public Collection<Rank> getAll() {
        return ranksByIdentifier.values();
    }

    public void clear() {
        ranksByIdentifier.clear();
        ranksById.clear();
    }
}