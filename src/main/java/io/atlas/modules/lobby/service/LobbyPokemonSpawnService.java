package io.atlas.modules.lobby.service;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.entity.PokemonEntityLoadEvent;
import com.cobblemon.mod.common.api.events.entity.SpawnEvent;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.world.level.Level;
import net.minecraft.server.MinecraftServer;

import java.util.Set;

public class LobbyPokemonSpawnService {

    private static final Set<String> EMERALD_ALLOWED_SPECIES = Set.of(
            "bidoof",
            "bunnelby",
            "caterpie",
            "fletchling",
            "goldeen",
            "hoothoot",
            "lechonk",
            "lillipup",
            "magikarp",
            "patrat",
            "pidgey",
            "pidove",
            "pikipek",
            "rattata",
            "rookidee",
            "sentret",
            "skwovet",
            "starly",
            "tarountula",
            "weedle",
            "wurmple",
            "yungoos",
            "zigzagoon"
    );

    private ObservableSubscription<SpawnEvent<PokemonEntity>> spawnSubscription;
    private ObservableSubscription<PokemonEntityLoadEvent> loadSubscription;

    public void enable() {
        spawnSubscription = CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(event -> {
            if (!isAllowed(event.getEntity())) {
                event.cancel();
            }
        });

        loadSubscription = CobblemonEvents.POKEMON_ENTITY_LOAD.subscribe(event -> {
            if (!isAllowed(event.getPokemonEntity())) {
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

        var emerald = server.getLevel(LobbyWorlds.EMERALD);
        if (emerald != null) {
            emerald.getAllEntities().forEach(entity -> {
                if (entity instanceof PokemonEntity pokemon && !isAllowed(pokemon)) {
                    pokemon.discard();
                }
            });
        }
    }

    private boolean isAllowed(PokemonEntity pokemon) {
        Level level = pokemon.level();
        if (LobbyWorlds.isAuth(level)) {
            return false;
        }
        if (!LobbyWorlds.isEmerald(level)) {
            return true;
        }

        String species = pokemon.getPokemon()
                .getSpecies()
                .getResourceIdentifier()
                .getPath();
        return EMERALD_ALLOWED_SPECIES.contains(species);
    }
}
