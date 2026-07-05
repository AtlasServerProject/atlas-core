package io.atlas.modules.performance;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.performance.service.EntityCleanupService;
import io.atlas.modules.performance.service.TpsMonitorService;
import io.atlas.modules.performance.service.ItemRecoveryService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public final class PerformanceModule implements AtlasModule {
    private static final TpsMonitorService TPS = new TpsMonitorService();
    private static final ItemRecoveryService RECOVERY = new ItemRecoveryService();
    private static final EntityCleanupService CLEANUP = new EntityCleanupService(RECOVERY);
    public static TpsMonitorService getTpsMonitor() { return TPS; }
    public static EntityCleanupService getCleanupService() { return CLEANUP; }
    public static ItemRecoveryService getRecoveryService() { return RECOVERY; }
    @Override public String getName() { return "Performance"; }
    @Override public void enable() {
        ServerTickEvents.START_SERVER_TICK.register(TPS::startTick);
        ServerTickEvents.END_SERVER_TICK.register(CLEANUP::tick);
        ServerTickEvents.END_SERVER_TICK.register(RECOVERY::tick);
        ServerTickEvents.END_SERVER_TICK.register(TPS::endTick);
        AtlasMod.LOGGER.info("Monitoramento de TPS e limpeza segura iniciados.");
    }
    @Override public void disable() {}
}
