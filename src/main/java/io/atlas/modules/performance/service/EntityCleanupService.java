package io.atlas.modules.performance.service;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.atlas.AtlasMod;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class EntityCleanupService {
    private static final int INTERVAL_TICKS = 15 * 60 * 20;
    private static final Set<String> PROTECTED_POKEMON = Set.of(
            "arceus",
            "articuno",
            "azelf",
            "calyrex",
            "celebi",
            "chienpao",
            "chiyu",
            "cobalion",
            "cosmoem",
            "cosmog",
            "cresselia",
            "darkrai",
            "deoxys",
            "dialga",
            "diancie",
            "entei",
            "eternatus",
            "fezandipiti",
            "genesect",
            "giratina",
            "glastrier",
            "gougingfire",
            "groudon",
            "heatran",
            "hooh",
            "hoopa",
            "ironboulder",
            "ironcrown",
            "jirachi",
            "keldeo",
            "koraidon",
            "kubfu",
            "kyogre",
            "kyurem",
            "landorus",
            "latias",
            "latios",
            "lugia",
            "lunala",
            "magearna",
            "manaphy",
            "marshadow",
            "melmetal",
            "meloetta",
            "meltan",
            "mesprit",
            "mew",
            "mewtwo",
            "miraidon",
            "moltres",
            "munkidori",
            "necrozma",
            "ogerpon",
            "okidogi",
            "palkia",
            "pecharunt",
            "phione",
            "ragingbolt",
            "raikou",
            "rayquaza",
            "regice",
            "regidrago",
            "regieleki",
            "regigigas",
            "regirock",
            "registeel",
            "reshiram",
            "shaymin",
            "silvally",
            "solgaleo",
            "spectrier",
            "suicune",
            "tapubulu",
            "tapufini",
            "tapukoko",
            "tapulele",
            "terapagos",
            "terrakion",
            "thundurus",
            "tinglu",
            "tornadus",
            "typenull",
            "urshifu",
            "uxie",
            "victini",
            "virizion",
            "volcanion",
            "wochien",
            "xerneas",
            "yveltal",
            "zacian",
            "zamazenta",
            "zapdos",
            "zarude",
            "zekrom",
            "zeraora",
            "zygarde"
    );
    private int remaining = INTERVAL_TICKS;
    private final ItemRecoveryService recovery;

    public EntityCleanupService(ItemRecoveryService recovery) { this.recovery = recovery; }

    public void tick(MinecraftServer server) {
        remaining--;
        if (remaining == 60 * 20 || remaining == 30 * 20 || remaining == 10 * 20) {
            int seconds = remaining / 20;
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "§e[Atlas] Limpeza em todos os mundos (itens no chão e Pokémon selvagens) em §f" + seconds + " segundos§e."), false);
        }
        if (remaining > 0) return;
        CleanupResult result = cleanup(server);
        remaining = INTERVAL_TICKS;
        if (result.total() > 0) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "§a[Atlas] Limpeza concluída: §f" + result.items() + " drops §ae §f"
                            + result.pokemon() + " Pokémon selvagens§a."), false);
        }
    }

    public CleanupResult cleanup(MinecraftServer server) {
        List<Entity> remove = new ArrayList<>();
        java.util.Map<java.util.UUID, List<net.minecraft.world.item.ItemStack>> recoverable = new java.util.HashMap<>();
        int items = 0;
        int pokemon = 0;
        int scanned = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity.isRemoved()) continue;
                scanned++;
                if (entity instanceof ItemEntity item) {
                    if (item.getOwner() instanceof ServerPlayer owner) {
                        recoverable.computeIfAbsent(owner.getUUID(), ignored -> new ArrayList<>())
                                .add(item.getItem().copy());
                    }
                    remove.add(item);
                    items++;
                } else if (entity instanceof PokemonEntity creature && canRemove(creature)) {
                    remove.add(creature);
                    pokemon++;
                }
            }
        }
        remove.forEach(Entity::discard);
        recoverable.forEach((uuid, stacks) -> recovery.store(uuid, "AUTO_CLEANUP", stacks, server));
        CleanupResult result = new CleanupResult(items, pokemon, scanned);
        AtlasMod.LOGGER.info("Limpeza Atlas (todos os mundos carregados): {} drops e {} Pokémon removidos; {} entidades verificadas.",
                items, pokemon, scanned);
        return result;
    }

    public EntityCounts counts(MinecraftServer server) {
        int total = 0;
        int items = 0;
        int pokemon = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity.isRemoved()) continue;
                total++;
                if (entity instanceof ItemEntity) items++;
                if (entity instanceof PokemonEntity) pokemon++;
            }
        }
        return new EntityCounts(total, items, pokemon);
    }

    private boolean canRemove(PokemonEntity pokemon) {
        if (pokemon.getOwner() != null || pokemon.isBattling() || pokemon.isBusy()
                || pokemon.getTethering() != null) {
            return false;
        }
        return !PROTECTED_POKEMON.contains(speciesId(pokemon));
    }

    private String speciesId(PokemonEntity pokemon) {
        return pokemon.getPokemon().getSpecies().getResourceIdentifier().getPath();
    }

    public record CleanupResult(int items, int pokemon, int scanned) {
        public int total() { return items + pokemon; }
    }
    public record EntityCounts(int total, int items, int pokemon) {}
}
