package io.atlas.modules.tournament;
import io.atlas.module.AtlasModule;
import io.atlas.modules.tournament.service.TournamentService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
public final class TournamentModule implements AtlasModule {
 private static final TournamentService SERVICE=new TournamentService();public static TournamentService service(){return SERVICE;}
 public String getName(){return "Torneios Challonge";}
 public void enable(){ServerLifecycleEvents.SERVER_STARTED.register(SERVICE::start);ServerLifecycleEvents.SERVER_STOPPING.register(s->SERVICE.stop());}
 public void disable(){}
}
