package io.atlas.modules.auth.service;

import io.atlas.modules.lobby.service.LobbyWorlds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AuthProtectionService {

    private static final Set<String> AUTH_COMMANDS = Set.of("login", "register");
    private static final Component LOGIN_REQUIRED_MESSAGE = Component.literal(
            "§cVocê precisa se autenticar antes de fazer isso. "
                    + "§eUse /login ou /register."
    );
    private static final Component HUB_COMMAND_BLOCKED_MESSAGE = Component.literal(
            "§cComandos são bloqueados no Hub. §eApenas /login e /register são permitidos."
    );

    private final AuthService authService;

    public AuthProtectionService(AuthService authService) {
        this.authService = authService;
    }

    public boolean canChat(UUID playerUuid) {
        return authService.isAuthenticated(playerUuid);
    }

    public boolean canExecuteCommand(ServerPlayer player, String command) {
        boolean authCommand = AUTH_COMMANDS.contains(commandRoot(command));
        if (LobbyWorlds.isAuth(player.level())) {
            return authCommand;
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

    private String commandRoot(String command) {
        String normalized = command == null ? "" : command.stripLeading();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        int argumentSeparator = normalized.indexOf(' ');
        String root = argumentSeparator < 0
                ? normalized
                : normalized.substring(0, argumentSeparator);
        return root.toLowerCase(Locale.ROOT);
    }
}
