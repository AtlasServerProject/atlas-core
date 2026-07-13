package io.atlas.modules.moderation;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.moderation.listener.ModerationConnectionListener;
import io.atlas.modules.moderation.service.ModerationService;
import io.atlas.modules.rank.RankModule;

public class ModerationModule implements AtlasModule {

    private static final ModerationService SERVICE =
            new ModerationService(RankModule.getRankService());

    public static ModerationService getService() {
        return SERVICE;
    }

    @Override
    public String getName() {
        return "Moderation";
    }

    @Override
    public void enable() {
        ModerationConnectionListener.register(SERVICE);
        AtlasMod.LOGGER.info("Moderation Manager iniciado.");
    }

    @Override
    public void disable() {
    }
}
