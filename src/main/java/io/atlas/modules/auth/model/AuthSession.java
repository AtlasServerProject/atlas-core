package io.atlas.modules.auth.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthSession(
        long id,
        UUID playerUuid,
        String ipAddress,
        boolean authenticated,
        LocalDateTime loginAt,
        LocalDateTime expiresAt
) {
}
