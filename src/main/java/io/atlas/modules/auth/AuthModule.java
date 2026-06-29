package io.atlas.modules.auth;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.auth.listener.AuthConnectionListener;
import io.atlas.modules.auth.service.AuthService;

public class AuthModule implements AtlasModule {

    private static final AuthService authService = new AuthService();

    public static AuthService getAuthService() {
        return authService;
    }

    @Override
    public String getName() {
        return "Authentication";
    }

    @Override
    public void enable() {
        AuthConnectionListener.register(authService);
        AtlasMod.LOGGER.info("Authentication Manager iniciado.");
    }

    @Override
    public void disable() {
        authService.shutdown();
    }
}
