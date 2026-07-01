package io.atlas.modules.auth;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.auth.listener.AuthConnectionListener;
import io.atlas.modules.auth.listener.AuthMovementListener;
import io.atlas.modules.auth.listener.AuthSessionExpiryListener;
import io.atlas.modules.auth.service.AuthMovementLockService;
import io.atlas.modules.auth.service.AuthLobbySpawnService;
import io.atlas.modules.auth.service.AuthProtectionService;
import io.atlas.modules.auth.service.AuthService;
import io.atlas.modules.auth.service.AuthSessionExpiryService;
import io.atlas.modules.auth.service.AuthWelcomeService;

public class AuthModule implements AtlasModule {

    private static final AuthService authService = new AuthService();
    private static final AuthMovementLockService movementLockService =
            new AuthMovementLockService(authService);
    private static final AuthLobbySpawnService lobbySpawnService =
            new AuthLobbySpawnService();
    private static final AuthSessionExpiryService sessionExpiryService =
            new AuthSessionExpiryService(authService);
    private static final AuthWelcomeService welcomeService =
            new AuthWelcomeService(authService);
    private static final AuthProtectionService protectionService =
            new AuthProtectionService(authService);

    public static AuthService getAuthService() {
        return authService;
    }

    public static AuthProtectionService getProtectionService() {
        return protectionService;
    }

    @Override
    public String getName() {
        return "Authentication";
    }

    @Override
    public void enable() {
        AuthConnectionListener.register(authService, lobbySpawnService, welcomeService);
        AuthSessionExpiryListener.register(sessionExpiryService);
        AuthMovementListener.register(movementLockService);
        AtlasMod.LOGGER.info("Authentication Manager iniciado.");
    }

    @Override
    public void disable() {
        authService.shutdown();
        movementLockService.clear();
    }
}
