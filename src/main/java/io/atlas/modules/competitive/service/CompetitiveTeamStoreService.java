package io.atlas.modules.competitive.service;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import java.util.Set;
import java.util.UUID;

/** Mirrors the native adjusted-level battle stores without changing Pokémon levels. */
public final class CompetitiveTeamStoreService {
    private CompetitiveTeamStoreService() {}

    public static void attach(PokemonBattle battle, UUID first, UUID second) {
        if (!battle.getBattlePartyStores().isEmpty()) {
            throw new IllegalStateException("Equipes temporárias já configuradas");
        }
        var players = Set.of(first, second);
        for (var actor : battle.getActors()) {
            if (!players.contains(actor.getUuid())) continue;
            var store = new PlayerPartyStore(actor.getUuid());
            int slot = 0;
            for (var member : actor.getPokemonList()) {
                var clone = member.getEffectedPokemon();
                if (clone == member.getOriginalPokemon()) {
                    throw new IllegalStateException("Batalha competitiva exige cópias dos Pokémon");
                }
                store.set(slot++, clone);
                if (!actor.getUuid().equals(clone.getOwnerUUID())) {
                    throw new IllegalStateException("Cópia sem vínculo com o jogador");
                }
            }
            battle.getBattlePartyStores().add(store);
        }
        if (battle.getBattlePartyStores().size() != 2) {
            throw new IllegalStateException("Duas equipes temporárias são obrigatórias");
        }
    }
}
