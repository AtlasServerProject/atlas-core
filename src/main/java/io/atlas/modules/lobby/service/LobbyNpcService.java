package io.atlas.modules.lobby.service;

import com.mojang.authlib.GameProfile;
import io.atlas.modules.auth.service.AuthService;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class LobbyNpcService {

    private static final String AUTH_EMERALD_NPC_TAG = "atlas_npc_auth_emerald";
    private static final String AUTH_EMERALD_NPC_PROFILE_NAME = "LobbyEmerald";
    private static final double AUTH_EMERALD_NPC_X = 628.622;
    private static final double AUTH_EMERALD_NPC_Y = 123.0;
    private static final double AUTH_EMERALD_NPC_Z = 3535.462;
    private static final float AUTH_EMERALD_NPC_YAW = -90.0F;
    private static final int SYNC_INTERVAL_TICKS = 100;

    private final AuthService authService;
    private final ServerSelectorService selectorService;
    private int ticks;

    public LobbyNpcService(AuthService authService, ServerSelectorService selectorService) {
        this.authService = authService;
        this.selectorService = selectorService;
    }

    public void tick(MinecraftServer server) {
        ticks++;
        if (ticks < SYNC_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;
        ensureAuthEmeraldNpc(server);
    }

    public void ensureAuthEmeraldNpc(MinecraftServer server) {
        ServerLevel authLobby = server.overworld();
        boolean found = false;

        for (Entity entity : authLobby.getAllEntities()) {
            if (!entity.getTags().contains(AUTH_EMERALD_NPC_TAG)) {
                continue;
            }

            if (!(entity instanceof FakePlayer)) {
                entity.discard();
                continue;
            }

            if (found) {
                entity.discard();
                continue;
            }

            found = true;
            keepNpcLocked(entity);
        }

        if (!found) {
            spawnAuthEmeraldNpc(authLobby);
        }
    }

    public InteractionResult interact(ServerPlayer player, Level world, InteractionHand hand, Entity entity) {
        if (hand != InteractionHand.MAIN_HAND || !world.dimension().equals(Level.OVERWORLD)) {
            return InteractionResult.PASS;
        }
        if (!entity.getTags().contains(AUTH_EMERALD_NPC_TAG)) {
            return InteractionResult.PASS;
        }

        if (!authService.isAuthenticated(player.getUUID())) {
            player.displayClientMessage(
                    Component.literal("§cFaça login para acessar o Lobby Emerald."),
                    false
            );
            return InteractionResult.SUCCESS;
        }

        selectorService.selectEmerald(player);
        return InteractionResult.SUCCESS;
    }

    private void spawnAuthEmeraldNpc(ServerLevel level) {
        FakePlayer npc = FakePlayer.get(level, new GameProfile(
                UUID.nameUUIDFromBytes((
                        "atlas:npc:auth_emerald:" + System.nanoTime()
                ).getBytes(StandardCharsets.UTF_8)),
                AUTH_EMERALD_NPC_PROFILE_NAME
        ));

        npc.teleportTo(AUTH_EMERALD_NPC_X, AUTH_EMERALD_NPC_Y, AUTH_EMERALD_NPC_Z);
        npc.setYRot(AUTH_EMERALD_NPC_YAW);
        npc.setXRot(0.0F);
        npc.addTag(AUTH_EMERALD_NPC_TAG);
        npc.setCustomName(Component.literal("§aLobby Emerald §7(clique)"));
        npc.setCustomNameVisible(true);
        keepNpcLocked(npc);

        if (!level.addFreshEntity(npc)) {
            level.addNewPlayer(npc);
        }
    }

    private void keepNpcLocked(Entity entity) {
        entity.setInvulnerable(true);
        entity.setSilent(true);
        entity.setNoGravity(true);
        entity.setPos(AUTH_EMERALD_NPC_X, AUTH_EMERALD_NPC_Y, AUTH_EMERALD_NPC_Z);
        entity.setYRot(AUTH_EMERALD_NPC_YAW);
        entity.setXRot(0.0F);
        entity.setCustomName(Component.literal("§aLobby Emerald §7(clique)"));
        entity.setCustomNameVisible(true);
    }
}
