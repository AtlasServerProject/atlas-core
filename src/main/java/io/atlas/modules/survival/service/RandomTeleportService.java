package io.atlas.modules.survival.service;

import io.atlas.AtlasMod;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.rank.model.Rank;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RandomTeleportService {

    private static final int MIN_RADIUS = 500;
    private static final int MAX_RADIUS = 2_850;
    private static final int QUEUE_TARGET = 32;
    private static final int REFILL_ATTEMPTS_PER_TICK = 16;
    private static final Map<UUID, Long> LAST_USE = new ConcurrentHashMap<>();
    private static final Map<UUID, SearchState> PENDING = new ConcurrentHashMap<>();

    private final RankService rankService;
    private final Deque<BlockPos> destinations = new ArrayDeque<>();
    private boolean queueReadyLogged;

    public RandomTeleportService(RankService rankService) {
        this.rankService = rankService;
    }

    public boolean request(ServerPlayer player) {
        UUID uuid = player.getUUID();
        if (PENDING.containsKey(uuid)) {
            player.displayClientMessage(
                    Component.literal("§eO Atlas ainda está procurando um local seguro para você."),
                    false
            );
            return true;
        }

        int cooldownSeconds = cooldownSeconds(uuid);
        long now = System.currentTimeMillis();
        long remainingMillis = LAST_USE.getOrDefault(uuid, 0L)
                + cooldownSeconds * 1_000L - now;
        if (remainingMillis > 0) {
            player.displayClientMessage(
                    Component.literal("§eAguarde " + formatDuration(remainingMillis) + " para usar /rtp novamente."),
                    false
            );
            return false;
        }

        ServerLevel survival = player.getServer().getLevel(LobbyWorlds.SURVIVAL_EMERALD);
        BlockPos ready = pollSafeDestination(survival);
        if (survival != null && ready != null) {
            completeTeleport(player, survival, ready);
            return true;
        }

        PENDING.put(uuid, new SearchState());
        player.displayClientMessage(
                Component.literal("§aProcurando um local seguro no Survival Emerald..."),
                false
        );
        return true;
    }

    public void tick(MinecraftServer server) {
        ServerLevel survival = server.getLevel(LobbyWorlds.SURVIVAL_EMERALD);
        if (survival == null) {
            return;
        }

        refillQueue(survival);

        if (PENDING.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, SearchState> entry : PENDING.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !LobbyWorlds.isEmerald(player.level())) {
                continue;
            }

            BlockPos destination = pollSafeDestination(survival);
            if (destination == null) {
                continue;
            }

            PENDING.remove(entry.getKey());
            completeTeleport(player, survival, destination);
        }
    }

    public void clear() {
        PENDING.clear();
        destinations.clear();
    }

    private void refillQueue(ServerLevel level) {
        int attempts = 0;
        while (destinations.size() < QUEUE_TARGET && attempts < REFILL_ATTEMPTS_PER_TICK) {
            attempts++;
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double radius = Math.sqrt(level.random.nextDouble())
                    * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS;
            int x = Mth.floor(Math.cos(angle) * radius);
            int z = Mth.floor(Math.sin(angle) * radius);
            BlockPos candidate = findSafeDestinationAt(level, x, z);
            if (candidate != null) {
                destinations.addLast(candidate);
            }
        }

        if (!queueReadyLogged && destinations.size() >= QUEUE_TARGET) {
            queueReadyLogged = true;
            AtlasMod.LOGGER.info("Fila do /rtp pronta com {} destinos seguros.", destinations.size());
        }
    }

    private BlockPos pollSafeDestination(ServerLevel level) {
        if (level == null) {
            return null;
        }
        while (!destinations.isEmpty()) {
            BlockPos candidate = destinations.removeFirst();
            if (isStillSafe(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private BlockPos findSafeDestinationAt(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos feet = new BlockPos(x, y, z);

        return isStillSafe(level, feet) ? feet : null;
    }

    private boolean isStillSafe(ServerLevel level, BlockPos feet) {
        int y = feet.getY();
        BlockPos head = feet.above();
        BlockPos ground = feet.below();
        BlockState groundState = level.getBlockState(ground);
        BlockState feetState = level.getBlockState(feet);
        BlockState headState = level.getBlockState(head);

        if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 2) {
            return false;
        }
        if (!level.getWorldBorder().isWithinBounds(feet)) {
            return false;
        }
        if (!level.getFluidState(ground).isEmpty()
                || !level.getFluidState(feet).isEmpty()
                || !level.getFluidState(head).isEmpty()
                || groundState.getCollisionShape(level, ground).isEmpty()
                || isDangerous(groundState)
                || !feetState.getCollisionShape(level, feet).isEmpty()
                || !headState.getCollisionShape(level, head).isEmpty()) {
            return false;
        }
        return true;
    }

    private boolean isDangerous(BlockState state) {
        return state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.CAMPFIRE)
                || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.FIRE)
                || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.POWDER_SNOW);
    }

    private void completeTeleport(ServerPlayer player, ServerLevel level, BlockPos destination) {
        player.stopRiding();
        player.teleportTo(
                level,
                destination.getX() + 0.5,
                destination.getY(),
                destination.getZ() + 0.5,
                player.getYRot(),
                player.getXRot()
        );
        player.setDeltaMovement(0.0, 0.0, 0.0);
        LAST_USE.put(player.getUUID(), System.currentTimeMillis());
        player.displayClientMessage(Component.literal("§aBem-vindo ao Survival Emerald!"), false);
    }

    private int cooldownSeconds(UUID uuid) {
        return rankService.getPlayerRanks(uuid).stream()
                .max(Comparator.comparingInt(Rank::getPriority))
                .map(Rank::getIdentifier)
                .map(this::cooldownForIdentifier)
                .orElse(180);
    }

    private int cooldownForIdentifier(String identifier) {
        String normalized = identifier.toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        return switch (normalized) {
            case "owner", "dono", "admin", "adm" -> 0;
            case "mod", "moderator", "sup", "support",
                    "vip++", "vip_plus_plus", "vipplusplus" -> 60;
            case "vip+", "vip_plus", "vipplus" -> 110;
            case "vip" -> 150;
            default -> 180;
        };
    }

    private String formatDuration(long remainingMillis) {
        long totalSeconds = Math.max(1L, (remainingMillis + 999L) / 1_000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes == 0) {
            return seconds + "s";
        }
        return seconds == 0 ? minutes + "min" : minutes + "min " + seconds + "s";
    }

    private static final class SearchState {
    }
}
