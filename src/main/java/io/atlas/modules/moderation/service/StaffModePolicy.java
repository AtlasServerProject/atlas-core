package io.atlas.modules.moderation.service;

import java.util.Locale;
import java.util.Set;

/** Explicit command boundary while inspecting; aliases/namespaces are not implicitly trusted. */
public final class StaffModePolicy {
    private StaffModePolicy() {}
    private static final Set<String> ALLOWED = Set.of("staffmode", "staff", "freeze", "invsee", "endersee",
            "warn", "kick", "mute", "unmute", "ban", "atlasban", "unban", "atlasunban", "banip", "unbanip",
            "staffnotes", "history", "punishments", "help", "list", "msg", "tell", "w", "login", "register", "logout");
    public static boolean allows(String command) {
        if (command == null) return false;
        String root = command.stripLeading().replaceFirst("^/+", "").split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        return ALLOWED.contains(root);
    }
}
