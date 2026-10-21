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

    private static final java.util.Map<String, String[]> INFO_NPCS = java.util.Map.of(
            "atlas_npc_emerald_caixas", new String[]{"§6Caixas — Tobias", "§7As caixas e recompensas estão em preparação. Volte em breve!"},
            "atlas_npc_emerald_tutorial", new String[]{"§aTutorial — Professor Carvalho", "§eRascunho: Bem-vindo ao Atlas! Sou o Professor Carvalho.",
                    "§7Em breve vou apresentar os primeiros passos da sua jornada.", "§7Por enquanto, fale com o NPC Survival Emerald para explorar."},
            "atlas_npc_emerald_arena", new String[]{"§cArena — Alain", "§7A arena está em preparação. As batalhas deste NPC ainda não estão disponíveis."},
            "atlas_npc_emerald_mercado", new String[]{"§bMercado — Poké Mart", "§7O mercado está em preparação. Compras e vendas ainda não estão disponíveis aqui."});

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
        if(entity.getTags().contains("atlas_npc_emerald_mercado")&&io.atlas.modules.lobby.service.LobbyWorlds.isEmerald(world)){if(hand==InteractionHand.MAIN_HAND&&authService.isAuthenticated(player.getUUID()))io.atlas.modules.market.MarketModule.service().open(player,"ALL",false,0,"");return InteractionResult.SUCCESS;}
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
        String[] info = null;
        if (LobbyWorlds.isEmerald(world)) {
            String type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            if (type.equals("easy_npc:humanoid") || type.equals("easy_npc:humanoid_slim")) {
                for (var entry : INFO_NPCS.entrySet()) {
                    if (entity.getTags().contains(entry.getKey())) { info = entry.getValue(); break; }
                }
            }
        }
        if (!authNpc && !survivalNpc && info == null) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.SUCCESS;
        if (!authService.isAuthenticated(player.getUUID())) {
            player.displayClientMessage(Component.literal("§cFaça login para usar este NPC."), false);
            return InteractionResult.SUCCESS;
        }
        if (info != null) {
            for (String line : info) player.displayClientMessage(Component.literal(line), false);
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
