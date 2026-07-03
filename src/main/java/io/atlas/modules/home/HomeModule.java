package io.atlas.modules.home;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.home.service.HomeService;
import io.atlas.modules.rank.RankModule;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class HomeModule implements AtlasModule {

    private static final HomeService HOME_SERVICE = new HomeService(RankModule.getRankService());

    public static HomeService getHomeService() {
        return HOME_SERVICE;
    }

    @Override
    public String getName() {
        return "Homes";
    }

    @Override
    public void enable() {
        ServerTickEvents.END_SERVER_TICK.register(HOME_SERVICE::tick);
        AtlasMod.LOGGER.info("Sistema de homes iniciado.");
    }

    @Override
    public void disable() {
        HOME_SERVICE.clear();
    }
}
