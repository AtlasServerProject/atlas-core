package io.atlas.modules.competitive;
import io.atlas.module.AtlasModule;
import io.atlas.modules.competitive.service.CompetitiveService;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
public final class CompetitiveModule implements AtlasModule {
 private static final CompetitiveService SERVICE=new CompetitiveService();public static CompetitiveService service(){return SERVICE;}
 public String getName(){return "Cláusulas competitivas Gen 9";}
 public void enable(){
  ServerLifecycleEvents.SERVER_STARTED.register(SERVICE::start);ServerLifecycleEvents.SERVER_STOPPING.register(s->SERVICE.stop());ServerTickEvents.END_SERVER_TICK.register(SERVICE::tick);
  CobblemonEvents.HELD_ITEM_PRE.subscribe(e->{if(SERVICE.originalLocked(e.getPokemon()))e.cancel();});
  CobblemonEvents.MEGA_EVOLUTION.subscribe(e->SERVICE.forbidden(e.getBattle(),"Mega Evolução"));CobblemonEvents.ZPOWER_USED.subscribe(e->SERVICE.forbidden(e.getBattle(),"Z-Move"));CobblemonEvents.TERASTALLIZATION.subscribe(e->SERVICE.tera(e.getBattle()));
 }
 public void disable(){}
}
