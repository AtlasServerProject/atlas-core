package io.atlas.modules.survival.service;

import io.atlas.AtlasMod;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.survival.SurvivalModule;
import io.atlas.modules.rank.model.Rank;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import io.atlas.modules.survival.menu.RandomTeleportMenu;

import java.util.EnumMap;
import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RandomTeleportService {

    private static final int MIN_RADIUS = 500;
    private static final int MAX_RADIUS = 5_850;
    private static final int QUEUE_TARGET = 32;
    private static final int REFILL_ATTEMPTS_PER_TICK = 16;
    private static final int WARMUP_TICKS = 60;
    private static final double MOVEMENT_TOLERANCE_SQUARED = 0.01;
    private static final Map<UUID, Long> LAST_USE = new ConcurrentHashMap<>();
    private static final Map<UUID, SearchState> PENDING = new ConcurrentHashMap<>();

    private final RankService rankService;
    private final Map<RtpTarget, Deque<BlockPos>> destinations = new EnumMap<>(RtpTarget.class);
    private final Map<RtpTarget, Boolean> queueReadyLogged = new EnumMap<>(RtpTarget.class);
    private long ticks;

    public RandomTeleportService(RankService rankService) {
        this.rankService = rankService;
        for (RtpTarget target : RtpTarget.values()) {
            destinations.put(target, new ArrayDeque<>());
            queueReadyLogged.put(target, false);
        }
    }

    public void openMenu(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(9);
        container.setItem(2, createOption(RtpTarget.OVERWORLD));
        container.setItem(4, createOption(RtpTarget.NETHER));
        container.setItem(6, createOption(RtpTarget.END));
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new RandomTeleportMenu(
                        containerId,
                        inventory,
                        container,
                        this
                ),
                Component.literal("§2RTP — Survival Emerald")
        ));
    }

    public boolean request(ServerPlayer player, RtpTarget target) {
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

        ServerLevel level = level(player.getServer(), target);
        if (level == null) {
            player.displayClientMessage(
                    Component.literal("§cEsse mundo ainda não está disponível."),
                    false
            );
            return false;
        }

        BlockPos reservedDestination = pollSafeDestination(level, target);
        PENDING.put(uuid, new SearchState(
                player.getX(),
                player.getY(),
                player.getZ(),
                ticks + WARMUP_TICKS,
                target,
                reservedDestination
        ));
        player.displayClientMessage(
                Component.literal("§aRTP para " + target.displayName() + " preparado. §eNão se mova por 3 segundos."),
                false
        );
        return true;
    }

    public void tick(MinecraftServer server) {
        ticks++;
        for (RtpTarget target : RtpTarget.values()) {
            ServerLevel level = level(server, target);
            if (level != null) {
                refillQueue(level, target);
            }
        }

        if (PENDING.isEmpty()) {
            return;
        }

        for (Map.Entry<UUID, SearchState> entry : PENDING.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                continue;
            }
            SearchState search = entry.getValue();
            if (!canUseRtpHere(player) || hasMoved(player, search)) {
                cancel(player, entry.getKey(), search);
                continue;
            }
            if (ticks < search.readyAtTick) {
                continue;
            }

            ServerLevel level = level(server, search.target);
            if (level == null) {
                continue;
            }

            BlockPos destination = search.destination;
            if (destination != null && !isStillSafe(level, destination)) {
                destination = null;
            }
            if (destination == null) {
                destination = pollSafeDestination(level, search.target);
            }
            if (destination == null) {
                continue;
            }

            PENDING.remove(entry.getKey());
            completeTeleport(player, level, destination, search.target);
        }
    }

    private boolean hasMoved(ServerPlayer player, SearchState search) {
        double x = player.getX() - search.x;
        double y = player.getY() - search.y;
        double z = player.getZ() - search.z;
        return x * x + y * y + z * z > MOVEMENT_TOLERANCE_SQUARED;
    }

    private boolean canUseRtpHere(ServerPlayer player) {
        return LobbyWorlds.isEmerald(player.level())
                || LobbyWorlds.isSurvivalArea(player.level());
    }

    private void cancel(ServerPlayer player, UUID uuid, SearchState search) {
        PENDING.remove(uuid);
        if (search.destination != null) {
            destinations.get(search.target).addFirst(search.destination);
        }
        player.displayClientMessage(
                Component.literal("§cRTP cancelado porque você se moveu. Nenhum cooldown foi aplicado."),
                false
        );
    }

    public void clear() {
        PENDING.clear();
        destinations.values().forEach(Deque::clear);
    }

    private void refillQueue(ServerLevel level, RtpTarget target) {
        Deque<BlockPos> queue = destinations.get(target);
        int attempts = 0;
        while (queue.size() < QUEUE_TARGET && attempts < REFILL_ATTEMPTS_PER_TICK) {
            attempts++;
            double angle = level.random.nextDouble() * Math.PI * 2.0;
            double radius = Math.sqrt(level.random.nextDouble())
                    * (MAX_RADIUS - MIN_RADIUS) + MIN_RADIUS;
            int x = Mth.floor(Math.cos(angle) * radius);
            int z = Mth.floor(Math.sin(angle) * radius);
            BlockPos candidate = findSafeDestinationAt(level, x, z);
            if (candidate != null) {
                queue.addLast(candidate);
            }
        }

        if (!queueReadyLogged.get(target) && queue.size() >= QUEUE_TARGET) {
            queueReadyLogged.put(target, true);
            AtlasMod.LOGGER.info("Fila do /rtp pronta em {} com {} destinos seguros.",
                    target.displayName(), queue.size());
        }
    }

    private BlockPos pollSafeDestination(ServerLevel level, RtpTarget target) {
        if (level == null) {
            return null;
        }
        Deque<BlockPos> queue = destinations.get(target);
        while (!queue.isEmpty()) {
            BlockPos candidate = queue.removeFirst();
            if (isStillSafe(level, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private BlockPos findSafeDestinationAt(ServerLevel level, int x, int z) {
        level.getChunk(x >> 4, z >> 4);
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        int min = level.getMinBuildHeight() + 2;
        int max = Math.min(level.getMaxBuildHeight() - 2, top);
        for (int y = max; y >= min; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            if (isStillSafe(level, feet)) {
                return feet;
            }
        }
        return null;
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
                || groundState.is(Blocks.BEDROCK)
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

    private void completeTeleport(ServerPlayer player, ServerLevel level, BlockPos destination, RtpTarget target) {
        SurvivalModule.getBackService().remember(player);
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
        player.displayClientMessage(Component.literal("§aBem-vindo ao " + target.displayName() + "!"), false);
    }

    private ServerLevel level(MinecraftServer server, RtpTarget target) {
        return server.getLevel(target.levelKey());
    }

    private ItemStack createOption(RtpTarget target) {
        ItemStack stack = new ItemStack(target.icon());
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(target.title()));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
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
        private final double x;
        private final double y;
        private final double z;
        private final long readyAtTick;
        private final RtpTarget target;
        private final BlockPos destination;

        private SearchState(
                double x,
                double y,
                double z,
                long readyAtTick,
                RtpTarget target,
                BlockPos destination
        ) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.readyAtTick = readyAtTick;
            this.target = target;
            this.destination = destination;
        }
    }

    public enum RtpTarget {
        OVERWORLD("Overworld", "§aOverworld", Items.GRASS_BLOCK, LobbyWorlds.SURVIVAL_EMERALD),
        NETHER("Nether", "§cNether", Items.NETHERRACK, net.minecraft.world.level.Level.NETHER),
        END("The End", "§5The End", Items.END_STONE, net.minecraft.world.level.Level.END);

        private final String displayName;
        private final String title;
        private final net.minecraft.world.item.Item icon;
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> levelKey;

        RtpTarget(
                String displayName,
                String title,
                net.minecraft.world.item.Item icon,
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> levelKey
        ) {
            this.displayName = displayName;
            this.title = title;
            this.icon = icon;
            this.levelKey = levelKey;
        }

        public String displayName() {
            return displayName;
        }

        public String title() {
            return title;
        }

        public net.minecraft.world.item.Item icon() {
            return icon;
        }

        public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> levelKey() {
            return levelKey;
        }
    }
}
