package io.atlas.modules.pokedex;
import io.atlas.module.AtlasModule;
import io.atlas.modules.pokedex.service.PokedexService;
public final class PokedexModule implements AtlasModule {
 private static final io.atlas.modules.pokedex.service.DexRewardService REWARDS=new io.atlas.modules.pokedex.service.DexRewardService();
 public static io.atlas.modules.pokedex.service.DexRewardService rewards(){return REWARDS;}
 private static final io.atlas.modules.pokedex.service.TypeRouteService TYPES=new io.atlas.modules.pokedex.service.TypeRouteService();
 public static io.atlas.modules.pokedex.service.TypeRouteService types(){return TYPES;}
 private static final PokedexService SERVICE=new PokedexService();
 public static PokedexService service(){return SERVICE;}
 public String getName(){return "Academia Atlas";}
 public void enable(){net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(REWARDS::start);net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(server->REWARDS.stop());net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(SERVICE::tick);net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{SERVICE.remove(handler.player.getUUID());REWARDS.remove(handler.player.getUUID());TYPES.remove(handler.player.getUUID());});}
 public void disable(){}
}
