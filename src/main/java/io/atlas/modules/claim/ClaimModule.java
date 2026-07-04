package io.atlas.modules.claim;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.claim.listener.ClaimProtectionListener;
import io.atlas.modules.claim.service.ClaimService;
import io.atlas.modules.rank.RankModule;

public final class ClaimModule implements AtlasModule {
    private static final ClaimService SERVICE = new ClaimService(RankModule.getRankService());
    public static ClaimService getService() { return SERVICE; }
    @Override public String getName() { return "Claims"; }
    @Override public void enable() {
        ClaimProtectionListener.register(SERVICE);
        AtlasMod.LOGGER.info("Sistema de claims iniciado.");
    }
    @Override public void disable() { SERVICE.clear(); }
}
