package io.atlas.modules.auth;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.auth.listener.AuthConnectionListener;
import io.atlas.modules.auth.listener.AuthMovementListener;
import io.atlas.modules.auth.service.AuthMovementLockService;
import io.atlas.modules.auth.service.AuthProtectionService;
import io.atlas.modules.auth.service.AuthService;

public class AuthModule implements AtlasModule {

    private static final AuthService authService = new AuthService();
    private static final AuthMovementLockService movementLockService =
            new AuthMovementLockService(authService);
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
        AuthConnectionListener.register(authService);
        AuthMovementListener.register(movementLockService);
        AtlasMod.LOGGER.info("Authentication Manager iniciado.");
    }

    @Override
    public void disable() {
        authService.shutdown();
        movementLockService.clear();
    }
}
