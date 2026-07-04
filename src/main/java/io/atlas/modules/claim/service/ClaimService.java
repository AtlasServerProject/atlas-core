package io.atlas.modules.claim.service;

import io.atlas.modules.claim.model.Claim;
import io.atlas.modules.claim.model.TrustLevel;
import io.atlas.modules.claim.repository.ClaimRepository;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

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
        message(player, "§aClaim criada: §f" + width + "x" + length + " §7(" + area + " blocos§7).");
        return true;
    }

    public void inspect(ServerPlayer player, BlockPos pos) {
        claimAt(player, pos).ifPresentOrElse(claim -> message(player,
                "§6Claim de §f" + claim.ownerName() + " §7| §f" + claim.area() + " blocos §7| §f"
                        + claim.minX() + "," + claim.minZ() + " até " + claim.maxX() + "," + claim.maxZ()),
                () -> message(player, "§7Este bloco não está protegido."));
    }

    public boolean can(ServerPlayer player, BlockPos pos, TrustLevel required, boolean notify) {
        Optional<Claim> found = claimAt(player, pos);
        if (found.isEmpty()) return true;
        Claim claim = found.get();
        if (claim.ownerUuid().equals(player.getUUID()) || rankService.canManageRanks(player.getUUID())) return true;
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
    public void clear() { selections.clear(); }

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
}
