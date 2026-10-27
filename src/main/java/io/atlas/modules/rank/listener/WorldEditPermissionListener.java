package io.atlas.modules.rank.listener;

import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rank.service.RankService;
import io.atlas.modules.rank.service.WorldEditPermissionService;
import me.lucko.fabric.api.permissions.v0.PermissionCheckEvent;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class WorldEditPermissionListener {
    private WorldEditPermissionListener() {}
    public static void register(RankService ranks) {
        var permissions = new WorldEditPermissionService(
                uuid -> AuthModule.getAuthService().isAuthenticated(uuid),
                uuid -> ranks.canManageRanks(uuid) && ranks.hasPermission(uuid, "worldedit.*"),
                System::nanoTime);
        Map<UUID, Boolean> published = new HashMap<>();
        PermissionCheckEvent.EVENT.register((source, node) -> {
            if (!(source instanceof net.minecraft.commands.CommandSourceStack stack)
                    || !(stack.getEntity() instanceof ServerPlayer player)) return TriState.DEFAULT;
            return switch (permissions.check(player.getUUID(), node)) {
                case ALLOW -> TriState.TRUE;
                case DENY -> TriState.FALSE;
                case DEFAULT -> TriState.DEFAULT;
            };
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 != 0) return;
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                boolean allowed = permissions.allowed(player.getUUID());
                Boolean previous = published.put(player.getUUID(), allowed);
                if (previous == null || previous != allowed) server.getCommands().sendCommands(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            permissions.forget(handler.player.getUUID());
            published.remove(handler.player.getUUID());
        });
        io.atlas.AtlasMod.LOGGER.info("WorldEdit integrado aos cargos ADM/Dono autenticados, sem conceder OP.");
    }
}
