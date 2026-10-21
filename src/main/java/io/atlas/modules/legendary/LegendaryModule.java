package io.atlas.modules.legendary;
import io.atlas.module.AtlasModule;
import io.atlas.modules.legendary.service.LegendaryService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.cobblemon.mod.common.api.events.CobblemonEvents;

public final class LegendaryModule implements AtlasModule {
    private static final LegendaryService SERVICE=new LegendaryService();
    public static LegendaryService service(){return SERVICE;}
    public String getName(){return "Lendários";}
    public void enable(){
        try{io.atlas.modules.legendary.config.LegendaryModelRegistry.available();}catch(Exception error){io.atlas.AtlasMod.LOGGER.error("Falha ao validar modelos lendários.",error);}
        ServerLifecycleEvents.SERVER_STARTED.register(server->{io.atlas.commands.LegendaryCommand.attachPreview(server.getCommands().getDispatcher());SERVICE.start(server);});
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server,resources,success)->{if(success)io.atlas.commands.LegendaryCommand.attachPreview(server.getCommands().getDispatcher());});
        ServerTickEvents.END_SERVER_TICK.register(SERVICE::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server->SERVICE.stop());
        CobblemonEvents.POKEMON_CAPTURED.subscribe(event->{io.atlas.modules.legendary.service.LegendaryIvService.ensure(event.getPokemon());SERVICE.captured(event);});
        CobblemonEvents.POKEMON_ENTITY_LOAD.subscribe(event->{io.atlas.modules.legendary.service.LegendaryIvService.ensure(event.getPokemonEntity().getPokemon());SERVICE.loaded(event.getPokemonEntity());});
        CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(event->{io.atlas.modules.legendary.service.LegendaryIvService.ensure(event.getEntity().getPokemon());});
    }
    public void disable(){}
}
