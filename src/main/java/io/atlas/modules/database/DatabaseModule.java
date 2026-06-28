package io.atlas.modules.database;

import io.atlas.module.AtlasModule;

public class DatabaseModule implements AtlasModule {

    @Override
    public String getName() {
        return "Database";
    }

    @Override
    public void enable() {
        DatabaseManager.connect();
    }

    @Override
    public void disable() {
        DatabaseManager.disconnect();
    }
}