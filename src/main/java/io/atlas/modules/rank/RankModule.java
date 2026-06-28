package io.atlas.modules.rank;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.rank.listener.RankSyncListener;
import io.atlas.modules.rank.service.PlayerDisplayService;
import io.atlas.modules.rank.service.RankService;
import io.atlas.modules.rank.service.RankSyncService;

public class RankModule implements AtlasModule {

    private static final RankService rankService = new RankService();
    private static final PlayerDisplayService displayService =
            new PlayerDisplayService(rankService);
    private static final RankSyncService syncService =
            new RankSyncService(rankService, displayService);

    public static RankService getRankService() {
        return rankService;
    }

    public static PlayerDisplayService getDisplayService() {
        return displayService;
    }

    @Override
    public String getName() {
        return "Ranks";
    }

    @Override
    public void enable() {
        rankService.loadRanks();
        RankSyncListener.register(syncService);
        AtlasMod.LOGGER.info("Rank Manager iniciado.");
    }

    @Override
    public void disable() {
    }
}
