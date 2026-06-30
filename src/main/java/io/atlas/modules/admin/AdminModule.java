package io.atlas.modules.admin;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.admin.listener.AdminServerLifecycleListener;
import io.atlas.modules.admin.service.AdminConsoleService;

public class AdminModule implements AtlasModule {

    private static final AdminConsoleService consoleService =
            new AdminConsoleService();

    @Override
    public String getName() {
        return "Admin Console";
    }

    @Override
    public void enable() {
        AdminServerLifecycleListener.register(consoleService);
        AtlasMod.LOGGER.info("Console administrativo Atlas registrado.");
    }

    @Override
    public void disable() {
        consoleService.stop();
    }
}
