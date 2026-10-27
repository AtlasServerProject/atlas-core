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
    private final LobbyNpcDestinationService destinationService = new LobbyNpcDestinationService();
    private final ServerSelectorService selectorService;

    public LobbyNpcService(AuthService authService, ServerSelectorService selectorService) {
        this.authService = authService;
        this.selectorService = selectorService;
    }

    public void tick(MinecraftServer server) {
        // Visual NPCs are handled by an external Fabric NPC mod.
        // Atlas Core owns navigation; Easy NPC retains appearance and persistent positions.
    }

    public void ensureAuthEmeraldNpc(MinecraftServer server) {
        // Intentionally empty. The NPC body/skin is managed by Easy NPC.
    }

    public InteractionResult interact(ServerPlayer player, Level world, InteractionHand hand, Entity entity) {
        if(entity.getTags().contains("atlas_npc_hall_professores")){
            if(hand==InteractionHand.MAIN_HAND){
                if(authService.isAuthenticated(player.getUUID()))io.atlas.modules.pokedex.PokedexModule.service().hall(player,0);
                else player.sendSystemMessage(Component.literal("§cFaça login para consultar o Hall."));
            }
            return InteractionResult.SUCCESS;
        }
        boolean authNpc = world.dimension().equals(Level.OVERWORLD)
                && entity.getTags().contains(AUTH_EMERALD_NPC_TAG);
        boolean survivalNpc = LobbyWorlds.isEmerald(world)
                && entity.getTags().contains("atlas_npc_emerald_survival")
                && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                    .toString().equals("easy_npc:humanoid");
        String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String destinationTag = LobbyWorlds.isEmerald(world)
                && (type.equals("easy_npc:humanoid") || type.equals("easy_npc:humanoid_slim"))
                ? LobbyNpcDestinationService.destinationTag(entity.getTags()) : null;
        if (!authNpc && !survivalNpc && destinationTag == null) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.SUCCESS;
        if (!authService.isAuthenticated(player.getUUID())) {
            player.displayClientMessage(Component.literal("§cFaça login para usar este NPC."), false);
            return InteractionResult.SUCCESS;
        }
        if (destinationTag != null) {
            destinationService.travel(player, destinationTag);
        } else if (survivalNpc) {
            var rtp = io.atlas.modules.survival.SurvivalModule.getRandomTeleportService();
            if (!(player.containerMenu instanceof io.atlas.modules.survival.menu.RandomTeleportMenu)) {
                rtp.openMenu(player);
            }
        } else {
            selectorService.openSelector(player);
        }
        return InteractionResult.SUCCESS;
    }
}
