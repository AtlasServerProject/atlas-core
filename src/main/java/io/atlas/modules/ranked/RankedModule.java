package io.atlas.modules.ranked;
import io.atlas.module.AtlasModule;
import io.atlas.modules.ranked.service.RankedService;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
public final class RankedModule implements AtlasModule {
 private static final RankedService SERVICE=new RankedService();
 public static RankedService service(){return SERVICE;}
 public String getName(){return "Ranking PvP independente";}
 public void enable(){ServerLifecycleEvents.SERVER_STARTED.register(SERVICE::start);ServerLifecycleEvents.SERVER_STOPPING.register(s->SERVICE.stop());ServerTickEvents.END_SERVER_TICK.register(SERVICE::tick);CobblemonEvents.BATTLE_VICTORY.subscribe(SERVICE::victory);}
 public void disable(){}
}
