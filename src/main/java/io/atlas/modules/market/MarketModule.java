package io.atlas.modules.market;
import io.atlas.module.AtlasModule;import io.atlas.modules.market.service.MarketService;
public final class MarketModule implements AtlasModule {
 private static final MarketService SERVICE=new MarketService();public static MarketService service(){return SERVICE;}
 public String getName(){return "Mercado e GTS";}
 public void enable(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(SERVICE::start);net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server->SERVICE.stop());}
 public void disable(){}
}
