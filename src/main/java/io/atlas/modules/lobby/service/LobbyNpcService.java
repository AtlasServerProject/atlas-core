package io.atlas.modules.lobby.service;

import io.atlas.modules.auth.service.AuthService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;

public class LobbyNpcService {

    private static final String AUTH_EMERALD_NPC_TAG = "atlas_npc_auth_emerald";

    private final AuthService authService;
    private final ServerSelectorService selectorService;

    public LobbyNpcService(AuthService authService, ServerSelectorService selectorService) {
        this.authService = authService;
        this.selectorService = selectorService;
    }

    public void tick(MinecraftServer server) {
        // Visual NPCs are handled by an external Fabric NPC mod.
        // Atlas Core only owns the click behavior for entities tagged with AUTH_EMERALD_NPC_TAG.
    }

    public void ensureAuthEmeraldNpc(MinecraftServer server) {
        // Intentionally empty. The NPC body/skin is managed by Easy NPC.
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
}
