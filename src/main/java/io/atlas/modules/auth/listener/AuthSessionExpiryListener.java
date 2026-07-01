package io.atlas.modules.auth.listener;

import io.atlas.modules.auth.service.AuthSessionExpiryService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class AuthSessionExpiryListener {

    public static void register(AuthSessionExpiryService expiryService) {
        ServerTickEvents.END_SERVER_TICK.register(expiryService::tick);
    }
}
