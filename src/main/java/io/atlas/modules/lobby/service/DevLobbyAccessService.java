package io.atlas.modules.lobby.service;

import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rank.RankModule;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class DevLobbyAccessService {
    private static final io.atlas.modules.rank.service.PlayerPermissionService personalPermissions =
            new io.atlas.modules.rank.service.PlayerPermissionService();

    public static boolean isAuthorized(java.util.UUID uuid) {
        return RankModule.getRankService().canManageRanks(uuid)
                || personalPermissions.hasPermission(uuid, "atlas.mundodev");
    }

    public static void forget(java.util.UUID uuid) { personalPermissions.forget(uuid); }

    public boolean canAccess(ServerPlayer player) {
        return AuthModule.getAuthService().isAuthenticated(player.getUUID())
                && (DevEnvironment.DEVELOPMENT
                    ? DevTransferService.identity(player.getUUID()) != null
                    : isAuthorized(player.getUUID()));
    }

    public int enter(ServerPlayer player) {
        // Revalidar ao executar: o cargo pode ter sido removido desde que o
        // cliente recebeu a árvore de comandos.
        if (!canAccess(player)) {
            player.sendSystemMessage(Component.literal(
                    "§cO /mundodev exige login e cargo Dono/ADM ou permissão individual."
            ));
            return 0;
        }

        return DevTransferService.enter(player);
    }
}
