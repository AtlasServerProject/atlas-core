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
    private static final Set<String> WORLDEDIT_COMMANDS = Set.of(
            "/biome",
            "/br",
            "/brush",
            "/calc",
            "/cancel",
            "/center",
            "/chunk",
            "/clipboard",
            "/contract",
            "/copy",
            "/count",
            "/curve",
            "/cut",
            "/cyl",
            "/deform",
            "/desel",
            "/deselect",
            "/drain",
            "/ex",
            "/expand",
            "/faces",
            "/fast",
            "/fill",
            "/fillr",
            "/fixlava",
            "/fixwater",
            "/flip",
            "/flora",
            "/forest",
            "/forestgen",
            "/gmask",
            "/green",
            "/hcyl",
            "/hollow",
            "/hpyramid",
            "/hsphere",
            "/inset",
            "/line",
            "/limit",
            "/mask",
            "/move",
            "/naturalize",
            "/outline",
            "/outset",
            "/overlay",
            "/paste",
            "/pos1",
            "/pos2",
            "/pyramid",
            "/redo",
            "/regen",
            "/removenear",
            "/replace",
            "/replacenear",
            "/rotate",
            "/schem",
            "/schematic",
            "/sel",
            "/set",
            "/shift",
            "/smooth",
            "/snow",
            "/sphere",
            "/stack",
            "/thaw",
            "/undo",
            "/walls",
            "ascend",
            "ceil",
            "chunkinfo",
            "clearclipboard",
            "copy",
            "descend",
            "distr",
            "farwand",
            "hpos1",
            "hpos2",
            "jumpto",
            "lrbuild",
            "none",
            "paste",
            "pos1",
            "pos2",
            "repl",
            "sel",
            "thru",
            "toggleplace",
            "tool",
            "unstuck",
            "up",
            "wand",
            "we",
            "worldedit"
    );
    private static final Component LOGIN_REQUIRED_MESSAGE = Component.literal(
            "§cVocê precisa se autenticar antes de fazer isso. "
                    + "§eUse /login ou /register."
    );
    private static final Component HUB_COMMAND_BLOCKED_MESSAGE = Component.literal(
            "§cComandos são bloqueados no Hub. §eApenas /login e /register são permitidos."
    );
    private static final Component WORLDEDIT_BLOCKED_MESSAGE = Component.literal(
            "§cWorldEdit é restrito a Dono e ADM."
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
        boolean authCommand = AUTH_COMMANDS.contains(commandRoot(command));
        if (isWorldEditCommand(command) && !rankService.canManageRanks(player.getUUID())) {
            return false;
        }
        if (LobbyWorlds.isAuth(player.level())) {
            return authCommand
                    || isAllowedWorldEditCommand(player, command)
                    || isAllowedDeveloperCommand(player, command);
        }
        return authService.isAuthenticated(player.getUUID()) || authCommand;
    }

    public Component loginRequiredMessage() {
        return LOGIN_REQUIRED_MESSAGE;
    }

    public Component commandBlockedMessage(ServerPlayer player) {
        return commandBlockedMessage(player, null);
    }

    public Component commandBlockedMessage(ServerPlayer player, String command) {
        if (isWorldEditCommand(command)) {
            return WORLDEDIT_BLOCKED_MESSAGE;
        }
        return LobbyWorlds.isAuth(player.level())
                ? HUB_COMMAND_BLOCKED_MESSAGE
                : LOGIN_REQUIRED_MESSAGE;
    }

    private boolean isAllowedWorldEditCommand(ServerPlayer player, String command) {
        return isWorldEditCommand(command) && rankService.canManageRanks(player.getUUID());
    }

    private boolean isAllowedDeveloperCommand(ServerPlayer player, String command) {
        return DEVELOPER_COMMANDS.contains(commandRoot(command))
                && rankService.canManageRanks(player.getUUID());
    }

    private boolean isWorldEditCommand(String command) {
        String root = commandRoot(command);
        return WORLDEDIT_COMMANDS.contains(root)
                || WORLDEDIT_COMMANDS.contains("/" + root);
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
