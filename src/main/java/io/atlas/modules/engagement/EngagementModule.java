package io.atlas.modules.engagement;

import io.atlas.module.AtlasModule;
import io.atlas.modules.engagement.service.EngagementService;
import net.fabricmc.fabric.api.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import com.cobblemon.mod.common.api.events.CobblemonEvents;

public final class EngagementModule implements AtlasModule {
    private static final EngagementService SERVICE=new EngagementService();
    public static EngagementService service(){return SERVICE;}
    public String getName(){return "Profissões e login diário";}
    public void enable(){
        ServerLifecycleEvents.SERVER_STARTED.register(SERVICE::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(server->SERVICE.stop());
        ServerTickEvents.END_SERVER_TICK.register(SERVICE::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->SERVICE.remove(handler.player.getUUID()));
        PlayerBlockBreakEvents.AFTER.register((world,player,pos,state,entity)->SERVICE.broken(world,player,pos,state));
        CobblemonEvents.POKEMON_CAPTURED.subscribe(SERVICE::captured);
        CobblemonEvents.BATTLE_VICTORY.subscribe(SERVICE::victory);
        CobblemonEvents.BOBBER_SPAWN_POKEMON_POST.subscribe(SERVICE::pokemonFished);
        CobblemonEvents.TRADE_EVENT_PRE.subscribe(event->{
            if(event.getTradeParticipant1Pokemon().getPersistentData().getBoolean("atlasGuaranteedHaReward")||event.getTradeParticipant2Pokemon().getPersistentData().getBoolean("atlasGuaranteedHaReward"))event.cancel();
        });
    }
    public void disable(){}
}
