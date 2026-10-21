package io.atlas.modules.research;
import io.atlas.module.AtlasModule;
import io.atlas.modules.research.service.ResearchService;
public final class ResearchModule implements AtlasModule {
 private static final ResearchService SERVICE=new ResearchService();public static ResearchService service(){return SERVICE;}
 public String getName(){return "Pesquisas Pokémon";}
 public void enable(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(SERVICE::start);net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server->SERVICE.stop());com.cobblemon.mod.common.api.events.CobblemonEvents.POKEMON_CAPTURED.subscribe(SERVICE::captured);net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->SERVICE.remove(handler.player.getUUID()));}
 public void disable(){}
}
