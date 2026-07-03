package io.atlas.modules.lobby.service;

import io.atlas.AtlasMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

public class SurvivalWorldService {

    public static final double WORLD_SIZE_BLOCKS = 6_000.0;
    public static final int WORLD_SIZE_CHUNKS = 375;
    public static final int PREGEN_RADIUS_BLOCKS = 3_000;

    public void configure(MinecraftServer server) {
        ServerLevel survival = server.getLevel(LobbyWorlds.SURVIVAL_EMERALD);
        if (survival == null) {
            AtlasMod.LOGGER.error("Dimensão atlas:survival_emerald não foi carregada.");
            return;
        }

        survival.getWorldBorder().setCenter(0.0, 0.0);
        survival.getWorldBorder().setSize(WORLD_SIZE_BLOCKS);
        AtlasMod.LOGGER.info(
                "Survival Emerald configurado com {}x{} chunks e borda de {} blocos.",
                WORLD_SIZE_CHUNKS,
                WORLD_SIZE_CHUNKS,
                (int) WORLD_SIZE_BLOCKS
        );
    }
}
