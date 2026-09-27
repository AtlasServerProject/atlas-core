package io.atlas.modules.moderation.service;

import io.atlas.AtlasMod;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.moderation.menu.InspectionMenu;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class StaffToolsService {
    private final RankService ranks;
    // Temporary moderation state: survives reconnects, cleared on server restart.
    private final Map<UUID, Anchor> frozen = java.util.Collections.synchronizedMap(new HashMap<>());
    private static final Set<String> AUTH_COMMANDS = Set.of("login", "register", "logout");

    public StaffToolsService(RankService ranks) { this.ranks = ranks; }

    public boolean canUse(CommandSourceStack source) {
        ServerPlayer actor = source.getPlayer();
        return actor == null || (authenticated(actor) && ranks.isStaff(actor.getUUID()));
    }

    private boolean authenticated(ServerPlayer player) {
        return AuthModule.getAuthService().isAuthenticated(player.getUUID());
    }

    public boolean canInspect(ServerPlayer actor, ServerPlayer target) {
        return authenticated(actor) && authenticated(target) && ranks.isStaff(actor.getUUID())
                && !LobbyWorlds.isAuth(actor.level()) && !LobbyWorlds.isAuth(target.level())
                && (actor.getUUID().equals(target.getUUID()) || higher(actor, target));
    }

    private boolean higher(ServerPlayer actor, ServerPlayer target) {
        return ranks.getHighestRank(actor.getUUID()).map(rank -> rank.getPriority()).orElse(0)
                > ranks.getHighestRank(target.getUUID()).map(rank -> rank.getPriority()).orElse(0);
    }

    public int freeze(CommandSourceStack source, ServerPlayer target) {
        ServerPlayer actor = source.getPlayer();
        if (!canUse(source) || (actor != null && (!higher(actor, target) || actor == target))) {
            source.sendFailure(Component.literal("§cVocê só pode congelar jogadores com cargo inferior ao seu."));
            return 0;
        }
        boolean enable = !isFrozen(target.getUUID());
        if (enable) {
            target.stopRiding();
            target.closeContainer();
            frozen.put(target.getUUID(), authenticated(target) ? anchor(target) : null);
        } else frozen.remove(target.getUUID());
        target.displayClientMessage(Component.literal(enable
                ? "§cVocê foi congelado pela staff. Aguarde as instruções no chat."
                : "§aVocê foi descongelado pela staff."), false);
        source.sendSuccess(() -> Component.literal("§a" + target.getName().getString()
                + (enable ? " congelado." : " descongelado.")), false);
        AtlasMod.LOGGER.info("[Staff] {} {} {}", source.getTextName(), enable ? "freeze" : "unfreeze", target.getUUID());
        return 1;
    }

    public int inspect(CommandSourceStack source, ServerPlayer target, boolean ender) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null || !canInspect(actor, target)) {
            source.sendFailure(Component.literal("§cConsulta disponível em jogo, fora do Auth Hub, para staff sobre jogadores de cargo inferior."));
            return 0;
        }
        InspectionMenu.open(actor, target, ender, this);
        AtlasMod.LOGGER.info("[Staff] {} {} {}", source.getTextName(), ender ? "endersee" : "invsee", target.getUUID());
        return 1;
    }

    public boolean isFrozen(UUID uuid) { return frozen.containsKey(uuid); }

    public boolean blocksCommand(ServerPlayer player, String command) {
        if (!isFrozen(player.getUUID())) return false;
        String root = command.stripLeading().replaceFirst("^/+", "").split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return !AUTH_COMMANDS.contains(root);
    }

    public void disconnected(ServerPlayer player) {
        if (isFrozen(player.getUUID())) {
            frozen.put(player.getUUID(), null);
            AtlasMod.LOGGER.warn("[Staff] Jogador congelado desconectou: {}", player.getUUID());
        }
    }

    public void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!isFrozen(player.getUUID())) continue;
            if (!authenticated(player)) { frozen.put(player.getUUID(), null); continue; }
            Anchor origin = frozen.get(player.getUUID());
            if (origin == null) { origin = anchor(player); frozen.put(player.getUUID(), origin); }
            player.stopRiding();
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
            if (player.serverLevel() != origin.level || player.position().distanceToSqr(origin.position) > 0.000001) {
                player.teleportTo(origin.level, origin.position.x, origin.position.y, origin.position.z,
                        player.getYRot(), player.getXRot());
            }
        }
    }

    public void clear() { frozen.clear(); }
    private Anchor anchor(ServerPlayer player) { return new Anchor(player.serverLevel(), player.position()); }
    private record Anchor(ServerLevel level, Vec3 position) {}
}
