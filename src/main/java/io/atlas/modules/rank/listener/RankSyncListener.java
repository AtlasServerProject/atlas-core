package io.atlas.modules.rank.listener;

import io.atlas.modules.rank.service.RankSyncService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class RankSyncListener {

    public static void register(RankSyncService syncService) {
        ServerTickEvents.END_SERVER_TICK.register(syncService::tick);
    }
}
