package io.atlas.modules.home.service;

import io.atlas.modules.home.model.Home;
import io.atlas.modules.home.repository.HomeRepository;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.rank.model.Rank;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class HomeService {

    private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z0-9_-]{1,16}");
    private static final int WARMUP_TICKS = 60;
    private static final double MOVEMENT_TOLERANCE_SQUARED = 0.01;

    private final HomeRepository repository = new HomeRepository();
    private final RankService rankService;
    private final Map<UUID, PendingTeleport> pending = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastTeleport = new ConcurrentHashMap<>();
    private long ticks;

    public HomeService(RankService rankService) {
        this.rankService = rankService;
    }

    public SetResult setHome(ServerPlayer player, String requestedName) {
        if (!LobbyWorlds.isSurvivalEmerald(player.level())) {
            return SetResult.SURVIVAL_ONLY;
        }
        String name = normalizeName(requestedName);
        if (!VALID_NAME.matcher(name).matches()) {
            return SetResult.INVALID_NAME;
        }

        Optional<Home> existing = repository.find(player.getUUID(), name);
        int currentHomes = repository.count(player.getUUID());
        if (existing.isEmpty() && currentHomes >= homeLimit(player.getUUID())) {
            return SetResult.LIMIT_REACHED;
        }

        repository.save(
                player.getUUID(),
                new Home(
                        existing.map(Home::id).orElse(0L),
                        name,
                        LobbyWorlds.SURVIVAL_EMERALD.location().toString(),
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        player.getYRot(),
                        player.getXRot(),
                        currentHomes == 0
                ),
                currentHomes == 0
        );
        return existing.isPresent() ? SetResult.UPDATED : SetResult.CREATED;
    }

    public boolean deleteHome(UUID uuid, String requestedName) {
        return repository.delete(uuid, normalizeName(requestedName));
    }

    public List<Home> listHomes(UUID uuid) {
        return repository.findAll(uuid);
    }

    public int homeLimit(UUID uuid) {
        return highestRank(uuid)
                .map(Rank::getIdentifier)
                .map(this::limitForRank)
                .orElse(2);
    }

    public TeleportResult requestTeleport(ServerPlayer player, String requestedName) {
        UUID uuid = player.getUUID();
        if (pending.containsKey(uuid)) {
            return TeleportResult.ALREADY_PENDING;
        }

        long remainingMillis = remainingCooldownMillis(uuid);
        if (remainingMillis > 0) {
            player.displayClientMessage(
                    Component.literal("§eAguarde " + formatDuration(remainingMillis) + " para usar /home novamente."),
                    false
            );
            return TeleportResult.COOLDOWN;
        }

        Optional<Home> home = requestedName == null
                ? repository.findPrimary(uuid)
                : repository.find(uuid, normalizeName(requestedName));
        if (home.isEmpty()) {
            return TeleportResult.NOT_FOUND;
        }

        pending.put(uuid, new PendingTeleport(
                player.getX(),
                player.getY(),
                player.getZ(),
                ticks + WARMUP_TICKS,
                home.get()
        ));
        player.displayClientMessage(
                Component.literal("§aTeleporte preparado. §eNão se mova por 3 segundos."),
                false
        );
        return TeleportResult.STARTED;
    }

    public void tick(MinecraftServer server) {
        ticks++;
        if (pending.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, PendingTeleport> entry : pending.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            PendingTeleport teleport = entry.getValue();
            if (hasMoved(player, teleport)) {
                pending.remove(entry.getKey());
                player.displayClientMessage(
                        Component.literal("§cTeleporte cancelado porque você se moveu. Nenhum cooldown foi aplicado."),
                        false
                );
                continue;
            }
            if (ticks < teleport.readyAtTick) {
                continue;
            }

            ServerLevel level = server.getLevel(LobbyWorlds.SURVIVAL_EMERALD);
            BlockPos destination = level == null ? null : findSafeDestination(level, teleport.home);
            if (destination == null) {
                pending.remove(entry.getKey());
                player.displayClientMessage(
                        Component.literal("§cSua home está obstruída. Libere o local antes de tentar novamente."),
                        false
                );
                continue;
            }

            pending.remove(entry.getKey());
            player.stopRiding();
            player.setDeltaMovement(Vec3.ZERO);
            player.teleportTo(
                    level,
                    destination.getX() + 0.5,
                    destination.getY(),
                    destination.getZ() + 0.5,
                    teleport.home.yaw(),
                    teleport.home.pitch()
            );
            lastTeleport.put(entry.getKey(), System.currentTimeMillis());
            player.displayClientMessage(
                    Component.literal("§aTeleportado para a home §f" + teleport.home.name() + "§a."),
                    false
            );
        }
    }

    public void clear() {
        pending.clear();
    }

    private BlockPos findSafeDestination(ServerLevel level, Home home) {
        int baseX = (int) Math.floor(home.x());
        int baseY = (int) Math.floor(home.y());
        int baseZ = (int) Math.floor(home.z());
        level.getChunk(baseX >> 4, baseZ >> 4);

        for (int radius = 0; radius <= 3; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    for (int y = -2; y <= 2; y++) {
                        BlockPos candidate = new BlockPos(baseX + x, baseY + y, baseZ + z);
                        if (isSafe(level, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafe(ServerLevel level, BlockPos feet) {
        BlockPos ground = feet.below();
        BlockPos head = feet.above();
        BlockState groundState = level.getBlockState(ground);
        return level.getWorldBorder().isWithinBounds(feet)
                && level.getFluidState(ground).isEmpty()
                && level.getFluidState(feet).isEmpty()
                && level.getFluidState(head).isEmpty()
                && !groundState.getCollisionShape(level, ground).isEmpty()
                && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(head).getCollisionShape(level, head).isEmpty();
    }

    private boolean hasMoved(ServerPlayer player, PendingTeleport teleport) {
        double x = player.getX() - teleport.x;
        double y = player.getY() - teleport.y;
        double z = player.getZ() - teleport.z;
        return x * x + y * y + z * z > MOVEMENT_TOLERANCE_SQUARED;
    }

    private long remainingCooldownMillis(UUID uuid) {
        int cooldown = highestRank(uuid)
                .map(Rank::getIdentifier)
                .map(this::cooldownForRank)
                .orElse(30);
        return lastTeleport.getOrDefault(uuid, 0L) + cooldown * 1_000L
                - System.currentTimeMillis();
    }

    private Optional<Rank> highestRank(UUID uuid) {
        return rankService.getPlayerRanks(uuid).stream()
                .max(Comparator.comparingInt(Rank::getPriority));
    }

    private int limitForRank(String identifier) {
        String rank = normalizeRank(identifier);
        return switch (rank) {
            case "owner", "dono", "admin", "adm" -> 100;
            case "mod", "moderator", "sup", "support" -> 25;
            case "vip++", "vip_plus_plus", "vipplusplus" -> 12;
            case "vip+", "vip_plus", "vipplus" -> 8;
            case "vip" -> 5;
            default -> 2;
        };
    }

    private int cooldownForRank(String identifier) {
        String rank = normalizeRank(identifier);
        return switch (rank) {
            case "owner", "dono", "admin", "adm", "mod", "moderator", "sup", "support" -> 0;
            case "vip++", "vip_plus_plus", "vipplusplus" -> 5;
            case "vip+", "vip_plus", "vipplus" -> 10;
            case "vip" -> 15;
            default -> 30;
        };
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private String normalizeRank(String rank) {
        return rank.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String formatDuration(long remainingMillis) {
        long seconds = Math.max(1L, (remainingMillis + 999L) / 1_000L);
        return seconds + "s";
    }

    public enum SetResult {
        CREATED, UPDATED, INVALID_NAME, LIMIT_REACHED, SURVIVAL_ONLY
    }

    public enum TeleportResult {
        STARTED, ALREADY_PENDING, COOLDOWN, NOT_FOUND
    }

    private record PendingTeleport(
            double x,
            double y,
            double z,
            long readyAtTick,
            Home home
    ) {
    }
}
