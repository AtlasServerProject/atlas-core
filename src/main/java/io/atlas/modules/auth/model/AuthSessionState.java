package io.atlas.modules.auth.model;

import java.util.UUID;

public record AuthSessionState(
        AuthSession session,
        boolean registered,
        boolean premium
) {

    public UUID playerUuid() {
        return session.playerUuid();
    }

    public boolean authenticated() {
        return session.authenticated();
    }
}
