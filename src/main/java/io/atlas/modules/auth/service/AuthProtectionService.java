package io.atlas.modules.auth.service;

import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AuthProtectionService {

    private static final Set<String> AUTH_COMMANDS = Set.of("login", "register");
    private static final Set<String> DEVELOPER_COMMANDS = Set.of("dev", "op", "deop");
    private static final Component LOGIN_REQUIRED_MESSAGE = Component.literal(
            "§cVocê precisa se autenticar antes de fazer isso. "
                    + "§eUse /login ou /register."
    );
    private static final Component HUB_COMMAND_BLOCKED_MESSAGE = Component.literal(
            "§cComandos são bloqueados no Hub. §eApenas /login e /register são permitidos."
    );

    private final AuthService authService;
    private final RankService rankService;

    public AuthProtectionService(AuthService authService) {
        this.authService = authService;
        this.rankService = io.atlas.modules.rank.RankModule.getRankService();
    }

    public boolean canChat(UUID playerUuid) {
        return authService.isAuthenticated(playerUuid);
    }

    public boolean canExecuteCommand(ServerPlayer player, String command) {
        // O Dono possui liberdade total sobre os comandos registrados pelo
        // servidor, mas ainda precisa estar autenticado para atravessar a
        // proteção inicial do Auth Lobby.
        if (authService.isAuthenticated(player.getUUID())
                && rankService.isOwner(player.getUUID())) {
            return true;
        }

        boolean authCommand = AUTH_COMMANDS.contains(commandRoot(command));
        if (LobbyWorlds.isAuth(player.level())) {
            return authCommand
                    || isAllowedDeveloperCommand(player, command);
        }
        return authService.isAuthenticated(player.getUUID()) || authCommand;
    }

    public Component loginRequiredMessage() {
        return LOGIN_REQUIRED_MESSAGE;
    }

    public Component commandBlockedMessage(ServerPlayer player) {
        return LobbyWorlds.isAuth(player.level())
                ? HUB_COMMAND_BLOCKED_MESSAGE
                : LOGIN_REQUIRED_MESSAGE;
    }

    private boolean isAllowedDeveloperCommand(ServerPlayer player, String command) {
        return DEVELOPER_COMMANDS.contains(commandRoot(command))
                && rankService.canManageRanks(player.getUUID());
    }

    private String commandRoot(String command) {
        String normalized = command == null ? "" : command.stripLeading();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        int argumentSeparator = normalized.indexOf(' ');
        String root = argumentSeparator < 0
                ? normalized
                : normalized.substring(0, argumentSeparator);
        return root.toLowerCase(Locale.ROOT);
    }
}
