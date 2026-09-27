package io.atlas.modules.kit;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.kit.service.DailyKitService;

public final class KitModule implements AtlasModule {

    private static final DailyKitService DAILY_KIT_SERVICE = new DailyKitService();

    public static DailyKitService getDailyKitService() {
        return DAILY_KIT_SERVICE;
    }

    @Override
    public String getName() {
        return "Kits";
    }

    @Override
    public void enable() {
        AtlasMod.LOGGER.info("Sistema de kits iniciado.");
    }

    @Override
    public void disable() {
    }
}
