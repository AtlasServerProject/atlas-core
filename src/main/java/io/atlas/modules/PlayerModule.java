package io.atlas.modules.player;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.player.listener.PlayerJoinListener;

public class PlayerModule implements AtlasModule {

    @Override
    public String getName() {
        return "Players";
    }

    @Override
    public void enable() {
        PlayerJoinListener.register();
        AtlasMod.LOGGER.info("Player Manager iniciado.");
    }

    @Override
    public void disable() {
    }
}