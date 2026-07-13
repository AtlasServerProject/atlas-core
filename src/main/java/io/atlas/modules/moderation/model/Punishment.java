package io.atlas.modules.moderation.model;

import java.time.Instant;

public record Punishment(
        long id,
        PunishmentType type,
        String targetName,
        String targetIp,
        String actorName,
        String reason,
        Instant expiresAt,
        Instant revokedAt,
        String revokedByName,
        String revokedReason,
        Instant createdAt
) {
    public boolean permanent() {
        return expiresAt == null;
    }
}
