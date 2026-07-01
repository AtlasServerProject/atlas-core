package io.atlas.modules.auth.service;

import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class AuthProtectionService {

    private static final Set<String> AUTH_COMMANDS = Set.of("login", "register");
    private static final Component LOGIN_REQUIRED_MESSAGE = Component.literal(
            "§cVocê precisa se autenticar antes de fazer isso. "
                    + "§eUse /login ou /register."
    );

    private final AuthService authService;

    public AuthProtectionService(AuthService authService) {
        this.authService = authService;
    }

    public boolean canChat(UUID playerUuid) {
        return authService.isAuthenticated(playerUuid);
    }

    public boolean canExecuteCommand(UUID playerUuid, String command) {
        return authService.isAuthenticated(playerUuid)
                || AUTH_COMMANDS.contains(commandRoot(command));
    }

    public Component loginRequiredMessage() {
        return LOGIN_REQUIRED_MESSAGE;
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
