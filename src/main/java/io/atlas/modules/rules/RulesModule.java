package io.atlas.modules.rules;

import io.atlas.module.AtlasModule;
import io.atlas.modules.rules.service.RulesService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public final class RulesModule implements AtlasModule {
    private static final RulesService SERVICE = new RulesService();
    public static RulesService service() { return SERVICE; }
    public String getName() { return "Regras e aceite obrigatório"; }
    public void enable() {
        ServerLifecycleEvents.SERVER_STARTED.register(SERVICE::start);
        ServerTickEvents.END_SERVER_TICK.register(SERVICE::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SERVICE.remove(handler.player.getUUID()));
    }
    public void disable() { SERVICE.clear(); }
}
