package io.atlas.modules.survival.service;

import io.atlas.modules.rank.model.Rank;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RandomTeleportService {

    private static final int MIN_RADIUS = 500;
    private static final int MAX_RADIUS = 2_850;
    private static final int MAX_ATTEMPTS = 32;
    private static final Map<UUID, Long> LAST_USE = new ConcurrentHashMap<>();

    private final RankService rankService;

    public RandomTeleportService(RankService rankService) {
        this.rankService = rankService;
    }

    public boolean teleport(ServerPlayer player) {
        int cooldownSeconds = cooldownSeconds(player.getUUID());
        long now = System.currentTimeMillis();
        long remainingMillis = LAST_USE.getOrDefault(player.getUUID(), 0L)
                + cooldownSeconds * 1_000L - now;
        if (remainingMillis > 0) {
            player.displayClientMessage(
                    Component.literal("§eAguarde " + formatDuration(remainingMillis) + " para usar /rtp novamente."),
                    false
            );
            return false;
        }

        ServerLevel level = (ServerLevel) player.level();
        BlockPos destination = findSafeDestination(level);
        if (destination == null) {
            player.displayClientMessage(
                    Component.literal("§cNão encontrei um local seguro. Tente novamente."),
                    false
            );
            return false;
        }

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
        LAST_USE.put(player.getUUID(), now);
        player.displayClientMessage(Component.literal("§aTeletransportado para uma área segura."), false);
        return true;
    }

    private BlockPos findSafeDestination(ServerLevel level) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double radius = Math.sqrt(level.random.nextDouble())
                    * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS;
            int x = Mth.floor(Math.cos(angle) * radius);
            int z = Mth.floor(Math.sin(angle) * radius);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos ground = feet.below();

            if (y <= level.getMinBuildHeight() + 1 || y >= level.getMaxBuildHeight() - 2) {
                continue;
            }
            if (!level.getWorldBorder().isWithinBounds(feet)) {
                continue;
            }
            if (level.getBlockState(ground).isAir()
                    || !level.getFluidState(ground).isEmpty()
                    || level.getBlockState(ground).is(Blocks.MAGMA_BLOCK)
                    || !level.getBlockState(feet).isAir()
                    || !level.getBlockState(feet.above()).isAir()) {
                continue;
            }
            return feet;
        }
        return null;
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
}
