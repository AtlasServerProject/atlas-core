package io.atlas.modules.rank;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.rank.service.RankService;

public class RankModule implements AtlasModule {

    private static final RankService rankService = new RankService();

    public static RankService getRankService() {
        return rankService;
    }

    @Override
    public String getName() {
        return "Ranks";
    }

    @Override
    public void enable() {
        rankService.loadRanks();
        AtlasMod.LOGGER.info("Rank Manager iniciado.");
    }

    @Override
    public void disable() {
    }
}