package io.atlas.modules.admin.listener;

import io.atlas.modules.admin.service.AdminConsoleService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class AdminServerLifecycleListener {

    private AdminServerLifecycleListener() {
    }

    public static void register(AdminConsoleService consoleService) {
        ServerLifecycleEvents.SERVER_STARTED.register(consoleService::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> consoleService.stop());
    }
}
