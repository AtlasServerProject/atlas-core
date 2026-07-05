package io.atlas.modules.performance.service;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.atlas.AtlasMod;
import io.atlas.modules.lobby.service.LobbyWorlds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;

public final class EntityCleanupService {
    private static final int INTERVAL_TICKS = 15 * 60 * 20;
    private static final int DROP_MIN_AGE = 5 * 60 * 20;
    private static final int POKEMON_MIN_AGE = 5 * 60 * 20;
    private static final double PLAYER_SAFE_DISTANCE_SQUARED = 64.0 * 64.0;
    private int remaining = INTERVAL_TICKS;
    private final ItemRecoveryService recovery;

    public EntityCleanupService(ItemRecoveryService recovery) { this.recovery = recovery; }

    public void tick(MinecraftServer server) {
        remaining--;
        if (remaining == 60 * 20 || remaining == 30 * 20 || remaining == 10 * 20) {
            int seconds = remaining / 20;
            server.getPlayerList().broadcastSystemMessage(Component.literal(
                    "§e[Atlas] Limpeza de entidades em §f" + seconds + " segundos§e."), false);
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
        ServerLevel survival = server.getLevel(LobbyWorlds.SURVIVAL_EMERALD);
        if (survival == null) return new CleanupResult(0, 0, 0);
        List<Entity> remove = new ArrayList<>();
        java.util.Map<java.util.UUID, List<net.minecraft.world.item.ItemStack>> recoverable = new java.util.HashMap<>();
        int items = 0;
        int pokemon = 0;
        int scanned = 0;
        for (Entity entity : survival.getAllEntities()) {
            scanned++;
            if (entity instanceof ItemEntity item && item.tickCount >= DROP_MIN_AGE) {
                if (item.getOwner() instanceof ServerPlayer owner) {
                    recoverable.computeIfAbsent(owner.getUUID(), ignored -> new ArrayList<>())
                            .add(item.getItem().copy());
                }
                remove.add(item);
                items++;
            } else if (entity instanceof PokemonEntity creature && canRemove(creature, survival)) {
                remove.add(creature);
                pokemon++;
            }
        }
        remove.forEach(Entity::discard);
        recoverable.forEach((uuid, stacks) -> recovery.store(uuid, "AUTO_CLEANUP", stacks, server));
        CleanupResult result = new CleanupResult(items, pokemon, scanned);
        AtlasMod.LOGGER.info("Limpeza Atlas: {} drops e {} Pokémon removidos; {} entidades verificadas.",
                items, pokemon, scanned);
        return result;
    }

    public EntityCounts counts(MinecraftServer server) {
        ServerLevel survival = server.getLevel(LobbyWorlds.SURVIVAL_EMERALD);
        if (survival == null) return new EntityCounts(0, 0, 0);
        int total = 0;
        int items = 0;
        int pokemon = 0;
        for (Entity entity : survival.getAllEntities()) {
            total++;
            if (entity instanceof ItemEntity) items++;
            if (entity instanceof PokemonEntity) pokemon++;
        }
        return new EntityCounts(total, items, pokemon);
    }

    private boolean canRemove(PokemonEntity pokemon, ServerLevel level) {
        if (pokemon.getOwner() != null || pokemon.isBattling() || pokemon.isBusy()
                || pokemon.getTethering() != null || pokemon.getTicksLived() < POKEMON_MIN_AGE) {
            return false;
        }
        for (ServerPlayer player : level.players()) {
            if (pokemon.distanceToSqr(player) <= PLAYER_SAFE_DISTANCE_SQUARED) return false;
        }
        return true;
    }

    public record CleanupResult(int items, int pokemon, int scanned) {
        public int total() { return items + pokemon; }
    }
    public record EntityCounts(int total, int items, int pokemon) {}
}
