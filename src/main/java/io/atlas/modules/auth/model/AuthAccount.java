package io.atlas.modules.auth.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record AuthAccount(
        long id,
        long playerId,
        UUID playerUuid,
        String passwordHash,
        String lastIp,
        boolean premium,
        boolean verified,
        LocalDateTime registeredAt,
        LocalDateTime lastLogin
) {
}
