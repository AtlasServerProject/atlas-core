package io.atlas.modules.claim.service;

import io.atlas.modules.claim.model.Claim;
import io.atlas.modules.claim.model.TrustLevel;
import io.atlas.modules.claim.repository.ClaimRepository;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class ClaimService {
    private static final int MIN_SIDE = 10;
    private final ClaimRepository repository = new ClaimRepository();
    private final RankService rankService;
    private final Map<UUID, BlockPos> selections = new HashMap<>();
    private final Map<String, Long> recentAudits = new HashMap<>();
    private final Map<UUID, BoundaryView> boundaryViews = new HashMap<>();

    public ClaimService(RankService rankService) {
        this.rankService = rankService;
    }

    public boolean select(ServerPlayer player, BlockPos pos) {
        if (!LobbyWorlds.isSurvivalEmerald(player.level())) {
            message(player, "§cClaims só podem ser criadas no Survival Emerald.");
            return false;
        }
        BlockPos first = selections.remove(player.getUUID());
        if (first == null) {
            selections.put(player.getUUID(), pos.immutable());
            showMarker(player, pos);
            message(player, "§aPrimeiro canto selecionado em §f" + coordinates(pos) + "§a.");
            return true;
        }
        int minX = Math.min(first.getX(), pos.getX());
        int maxX = Math.max(first.getX(), pos.getX());
        int minZ = Math.min(first.getZ(), pos.getZ());
        int maxZ = Math.max(first.getZ(), pos.getZ());
        int width = maxX - minX + 1;
        int length = maxZ - minZ + 1;
        if (width < MIN_SIDE || length < MIN_SIDE) {
            message(player, "§cA claim precisa ter no mínimo 10x10 blocos. Selecione novamente.");
            return false;
        }
        String world = player.level().dimension().location().toString();
        if (repository.overlaps(world, minX, minZ, maxX, maxZ)) {
            message(player, "§cEssa área encosta ou sobrepõe uma claim existente.");
            return false;
        }
        int area = width * length;
        int limit = areaLimit(player.getUUID());
        int used = repository.usedArea(player.getUUID());
        if (used + area > limit) {
            message(player, "§cLimite excedido: §f" + used + "/" + limit + " §cblocos já utilizados.");
            return false;
        }
        repository.create(player.getUUID(), world, minX, minZ, maxX, maxZ);
        showBoundary(player, new Claim(-1, player.getUUID(), player.getGameProfile().getName(), world,
                minX, minZ, maxX, maxZ));
        message(player, "§aClaim criada: §f" + width + "x" + length + " §7(" + area + " blocos§7).");
        return true;
    }

    public void inspect(ServerPlayer player, BlockPos pos) {
        claimAt(player, pos).ifPresentOrElse(claim -> {
            message(player, "§6Claim de §f" + claim.ownerName() + " §7| §f" + claim.area() + " blocos §7| §f"
                    + claim.minX() + "," + claim.minZ() + " até " + claim.maxX() + "," + claim.maxZ());
            showBoundary(player, claim);
        },
                () -> message(player, "§7Este bloco não está protegido."));
    }

    public boolean can(ServerPlayer player, BlockPos pos, TrustLevel required, boolean notify) {
        Optional<Claim> found = claimAt(player, pos);
        if (found.isEmpty()) return true;
        Claim claim = found.get();
        if (claim.ownerUuid().equals(player.getUUID())) return true;
        if (rankService.canManageRanks(player.getUUID())) {
            auditBypass(player, claim, required, pos);
            return true;
        }
        boolean allowed = repository.trust(claim.id(), player.getUUID())
                .map(level -> level.allows(required)).orElse(false);
        if (!allowed && notify) message(player, "§cEsta área é protegida por §f" + claim.ownerName() + "§c.");
        return allowed;
    }

    public boolean setTrust(ServerPlayer owner, String username, TrustLevel level) {
        Optional<Claim> claim = ownedClaimAt(owner);
        if (claim.isEmpty()) return false;
        boolean changed = repository.setTrust(claim.get().id(), username, level);
        message(owner, changed ? "§aPermissão " + level.name().toLowerCase(Locale.ROOT) + " concedida a §f" + username + "§a."
                : "§cJogador não encontrado. Ele precisa ter entrado no Atlas ao menos uma vez.");
        return changed;
    }

    public boolean removeTrust(ServerPlayer owner, String username) {
        Optional<Claim> claim = ownedClaimAt(owner);
        if (claim.isEmpty()) return false;
        boolean changed = repository.removeTrust(claim.get().id(), username);
        message(owner, changed ? "§aPermissões de §f" + username + " §aremovidas."
                : "§eEsse jogador não possuía permissão nesta claim.");
        return changed;
    }

    public boolean abandon(ServerPlayer player) {
        if (!LobbyWorlds.isSurvivalEmerald(player.level())) {
            message(player, "§cUse este comando dentro do Survival Emerald.");
            return false;
        }
        BlockPos pos = player.blockPosition();
        boolean deleted = repository.deleteOwnedAt(player.getUUID(), world(player), pos.getX(), pos.getZ());
        message(player, deleted ? "§aClaim removida." : "§cVocê precisa estar dentro de uma claim sua.");
        return deleted;
    }

    public List<Claim> list(UUID uuid) { return repository.findOwned(uuid); }

    public boolean teleportToClaim(ServerPlayer player, long claimId) {
        Optional<Claim> found = repository.findOwnedById(player.getUUID(), claimId);
        if (found.isEmpty()) {
            message(player, "§cEssa claim não existe ou não pertence a você.");
            return false;
        }
        var server = player.getServer();
        var level = server == null ? null : server.getLevel(LobbyWorlds.SURVIVAL_EMERALD);
        if (level == null) {
            message(player, "§cO Survival Emerald não está disponível agora.");
            return false;
        }
        BlockPos destination = safeDestination(level, found.get());
        if (destination == null) {
            message(player, "§cNão encontrei um local seguro dentro dessa claim.");
            return false;
        }
        player.stopRiding();
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(level, destination.getX() + 0.5, destination.getY(),
                destination.getZ() + 0.5, player.getYRot(), player.getXRot());
        message(player, "§aTeleportado para a claim §f#" + claimId + "§a.");
        return true;
    }
    public int areaLimit(UUID uuid) {
        if (rankService.canManageRanks(uuid)) return 1_000_000;
        return rankService.getHighestRank(uuid).map(rank -> switch (rank.getIdentifier().toUpperCase(Locale.ROOT)) {
            case "MOD", "MODERATOR", "SUP", "SUPPORT" -> 100_000;
            case "VIP++", "VIPPLUSPLUS" -> 50_000;
            case "VIP+", "VIPPLUS" -> 30_000;
            case "VIP" -> 20_000;
            default -> 10_000;
        }).orElse(10_000);
    }
    public boolean isClaimed(Level level, BlockPos pos) {
        if (!LobbyWorlds.isSurvivalEmerald(level)) return false;
        return repository.findAt(level.dimension().location().toString(), pos.getX(), pos.getZ()).isPresent();
    }

    public void tick(MinecraftServer server) {
        long now = System.currentTimeMillis();
        var iterator = boundaryViews.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (entry.getValue().expiresAt() > now) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) restore(player, entry.getValue());
            iterator.remove();
        }
    }

    public void clear() { selections.clear(); recentAudits.clear(); boundaryViews.clear(); }

    private Optional<Claim> ownedClaimAt(ServerPlayer player) {
        Optional<Claim> claim = claimAt(player, player.blockPosition());
        if (claim.isEmpty() || (!claim.get().ownerUuid().equals(player.getUUID())
                && !rankService.canManageRanks(player.getUUID()))) {
            message(player, "§cVocê precisa estar dentro de uma claim sua.");
            return Optional.empty();
        }
        return claim;
    }
    private Optional<Claim> claimAt(ServerPlayer player, BlockPos pos) {
        return repository.findAt(world(player), pos.getX(), pos.getZ());
    }
    private String world(ServerPlayer player) { return player.level().dimension().location().toString(); }
    private String coordinates(BlockPos pos) { return pos.getX() + ", " + pos.getY() + ", " + pos.getZ(); }
    private void message(ServerPlayer player, String text) { player.displayClientMessage(Component.literal(text), false); }

    private void auditBypass(ServerPlayer player, Claim claim, TrustLevel required, BlockPos pos) {
        String action = "BYPASS_" + required.name();
        String key = player.getUUID() + ":" + claim.id() + ":" + action;
        long now = System.currentTimeMillis();
        Long previous = recentAudits.put(key, now);
        if (previous != null && now - previous < 2_000L) return;
        repository.recordBypass(claim.id(), player.getUUID(), player.getGameProfile().getName(), action,
                world(player), pos.getX(), pos.getY(), pos.getZ());
    }

    private void showBoundary(ServerPlayer player, Claim claim) {
        int perimeter = Math.max(1, 2 * ((claim.maxX() - claim.minX()) + (claim.maxZ() - claim.minZ())));
        int step = Math.max(1, perimeter / 160);
        java.util.LinkedHashSet<BlockPos> positions = new java.util.LinkedHashSet<>();
        for (int x = claim.minX(); x <= claim.maxX(); x += step) {
            positions.add(surface(player, x, claim.minZ()));
            positions.add(surface(player, x, claim.maxZ()));
        }
        for (int z = claim.minZ(); z <= claim.maxZ(); z += step) {
            positions.add(surface(player, claim.minX(), z));
            positions.add(surface(player, claim.maxX(), z));
        }
        positions.add(surface(player, claim.minX(), claim.minZ()));
        positions.add(surface(player, claim.minX(), claim.maxZ()));
        positions.add(surface(player, claim.maxX(), claim.minZ()));
        positions.add(surface(player, claim.maxX(), claim.maxZ()));
        showFakeGold(player, positions, 15_000L);
    }

    private void showMarker(ServerPlayer player, BlockPos selected) {
        java.util.LinkedHashSet<BlockPos> positions = new java.util.LinkedHashSet<>();
        for (int offset = -2; offset <= 2; offset++) {
            positions.add(surface(player, selected.getX() + offset, selected.getZ()));
            positions.add(surface(player, selected.getX(), selected.getZ() + offset));
        }
        showFakeGold(player, positions, 10_000L);
    }

    private BlockPos surface(ServerPlayer player, int x, int z) {
        BlockPos probe = new BlockPos(x, player.blockPosition().getY(), z);
        if (!player.serverLevel().hasChunkAt(probe)) return null;
        int y = player.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        return new BlockPos(x, y, z);
    }

    private void showFakeGold(ServerPlayer player, java.util.Collection<BlockPos> positions, long duration) {
        BoundaryView previous = boundaryViews.remove(player.getUUID());
        if (previous != null) restore(player, previous);
        List<BlockPos> visible = positions.stream().filter(java.util.Objects::nonNull)
                .map(BlockPos::immutable).toList();
        for (BlockPos pos : visible) {
            player.connection.send(new ClientboundBlockUpdatePacket(pos, Blocks.GOLD_BLOCK.defaultBlockState()));
        }
        boundaryViews.put(player.getUUID(), new BoundaryView(player.level().dimension().location().toString(),
                visible, System.currentTimeMillis() + duration));
    }

    private void restore(ServerPlayer player, BoundaryView view) {
        if (!player.level().dimension().location().toString().equals(view.world())) return;
        for (BlockPos pos : view.positions()) {
            player.connection.send(new ClientboundBlockUpdatePacket(player.level(), pos));
        }
    }

    private BlockPos safeDestination(net.minecraft.server.level.ServerLevel level, Claim claim) {
        int centerX = claim.minX() + (claim.maxX() - claim.minX()) / 2;
        int centerZ = claim.minZ() + (claim.maxZ() - claim.minZ()) / 2;
        int maxRadius = Math.max(claim.maxX() - claim.minX(), claim.maxZ() - claim.minZ()) / 2;
        for (int radius = 0; radius <= maxRadius; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (radius > 0 && Math.abs(x) != radius && Math.abs(z) != radius) continue;
                    int targetX = centerX + x;
                    int targetZ = centerZ + z;
                    if (!claim.contains(targetX, targetZ)) continue;
                    level.getChunk(targetX >> 4, targetZ >> 4);
                    int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, targetX, targetZ);
                    BlockPos candidate = new BlockPos(targetX, y, targetZ);
                    if (isSafe(level, candidate)) return candidate;
                }
            }
        }
        return null;
    }

    private boolean isSafe(net.minecraft.server.level.ServerLevel level, BlockPos feet) {
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

    private record BoundaryView(String world, List<BlockPos> positions, long expiresAt) {}
}
