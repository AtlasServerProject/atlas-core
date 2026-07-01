package io.atlas.modules.lobby.service;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.entity.PokemonEntityLoadEvent;
import com.cobblemon.mod.common.api.events.entity.SpawnEvent;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;

public class LobbyPokemonSpawnService {

    private ObservableSubscription<SpawnEvent<PokemonEntity>> spawnSubscription;
    private ObservableSubscription<PokemonEntityLoadEvent> loadSubscription;

    public void enable() {
        spawnSubscription = CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(event -> {
            if (isAuthLobby(event.getEntity().level())) {
                event.cancel();
            }
        });

        loadSubscription = CobblemonEvents.POKEMON_ENTITY_LOAD.subscribe(event -> {
            if (isAuthLobby(event.getPokemonEntity().level())) {
                event.cancel();
            }
        });
    }

    public void disable() {
        if (spawnSubscription != null) {
            spawnSubscription.unsubscribe();
            spawnSubscription = null;
        }
        if (loadSubscription != null) {
            loadSubscription.unsubscribe();
            loadSubscription = null;
        }
    }

    public void tick(MinecraftServer server) {
        server.overworld().getAllEntities().forEach(entity -> {
            if (entity instanceof PokemonEntity) {
                entity.discard();
            }
        });
    }

    private boolean isAuthLobby(Level world) {
        return world.dimension() == Level.OVERWORLD;
    }
}
