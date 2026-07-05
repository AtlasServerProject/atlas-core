package io.atlas.modules.lobby.service;

import io.atlas.AtlasMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameRules;

public class SurvivalWorldService {

    public static final double WORLD_SIZE_BLOCKS = 12_000.0;
    public static final int WORLD_SIZE_CHUNKS = 750;
    public static final int PREGEN_RADIUS_BLOCKS = 6_000;
    private static final double SAFE_LIMIT = 5_990.0;
    private static final int CHECK_INTERVAL_TICKS = 20;
    private int ticks;

    public void configure(MinecraftServer server) {
        ServerLevel survival = server.getLevel(LobbyWorlds.SURVIVAL_EMERALD);
        if (survival == null) {
            AtlasMod.LOGGER.error("Dimensão atlas:survival_emerald não foi carregada.");
            return;
        }

        survival.getWorldBorder().setCenter(0.0, 0.0);
        survival.getWorldBorder().setSize(WORLD_SIZE_BLOCKS);
        survival.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS).set(true, server);
        AtlasMod.LOGGER.info(
                "Survival Emerald configurado com {}x{} chunks, borda de {} blocos e drops de blocos ativos.",
                WORLD_SIZE_CHUNKS,
                WORLD_SIZE_CHUNKS,
                (int) WORLD_SIZE_BLOCKS
        );
    }

    public void tick(MinecraftServer server) {
        ticks++;
        if (ticks < CHECK_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!LobbyWorlds.isSurvivalEmerald(player.level())) {
                continue;
            }

            double x = player.getX();
            double z = player.getZ();
            if (Math.abs(x) <= SAFE_LIMIT && Math.abs(z) <= SAFE_LIMIT) {
                continue;
            }

            double safeX = Mth.clamp(x, -SAFE_LIMIT, SAFE_LIMIT);
            double safeZ = Mth.clamp(z, -SAFE_LIMIT, SAFE_LIMIT);
            player.teleportTo(
                    (ServerLevel) player.level(),
                    safeX,
                    player.getY(),
                    safeZ,
                    player.getYRot(),
                    player.getXRot()
            );
            player.displayClientMessage(
                    Component.literal("§eVocê atingiu o limite do Survival Emerald."),
                    true
            );
        }
    }
}
