package io.atlas.modules.lobby.service;

import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.Cobblemon;
import io.atlas.AtlasMod;
import io.atlas.modules.lobby.repository.LobbyNpcDestinationRepository;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.Set;

public final class LobbyNpcDestinationService {
    private static final Set<String> TAGS = Set.of("atlas_npc_emerald_arena", "atlas_npc_emerald_mercado",
            "atlas_npc_emerald_tutorial", "atlas_npc_emerald_caixas", "atlas_npc_emerald_professor_carvalho");
    private final LobbyNpcDestinationRepository repository = new LobbyNpcDestinationRepository();
    private Map<String, LobbyNpcDestinationRepository.Destination> destinations = Map.of();
    private long reloadAt;

    public static String destinationTag(Set<String> entityTags) {
        for (String tag : TAGS) if (entityTags.contains(tag)) return tag;
        return null;
    }

    public void travel(ServerPlayer player, String tag) {
        // Already-selected players must get neither a screen nor a repeated NPC message.
        if (tag.equals("atlas_npc_emerald_professor_carvalho")) {
            var data = Cobblemon.INSTANCE.getPlayerDataManager().getGenericData(player);
            if (data.getStarterSelected() || data.getStarterUUID() != null) return;
        }
        if (BattleRegistry.getBattleByParticipatingPlayer(player) != null) {
            player.displayClientMessage(Component.literal("§cAguarde o fim da batalha para viajar."), false);
            return;
        }
        if (player.isPassenger()) {
            player.displayClientMessage(Component.literal("§eDesmonte antes de usar este NPC."), false);
            return;
        }
        try {
            long now = System.currentTimeMillis();
            if (now >= reloadAt) {
                destinations = repository.load();
                reloadAt = now + 60_000L;
            }
            var destination = destinations.get(tag);
            if (destination == null || !destination.world().equals("atlas:emerald")) {
                unavailable(player);
                return;
            }
            if (destination.action().equals("STARTER")) {
                if (!tag.equals("atlas_npc_emerald_professor_carvalho")) { unavailable(player); return; }
                // Native handler enforces locking, the configured roster and selection persistence.
                Cobblemon.INSTANCE.getStarterHandler().requestStarterChoice(player);
                return;
            }
            if (!destination.action().equals("TELEPORT")) { unavailable(player); return; }
            var key = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(destination.world()));
            var level = player.getServer().getLevel(key);
            if (level == null) { unavailable(player); return; }
            player.closeContainer();
            player.setDeltaMovement(Vec3.ZERO);
            player.fallDistance = 0;
            player.teleportTo(level, destination.x(), destination.y(), destination.z(), destination.yaw(), destination.pitch());
            player.displayClientMessage(Component.literal("§bVocê chegou: §f" + destination.label() + "§b."), true);
        } catch (Exception error) {
            // Brief failure cache prevents repeated database calls while unavailable.
            destinations = Map.of();
            reloadAt = System.currentTimeMillis() + 5_000L;
            AtlasMod.LOGGER.warn("Falha ao consultar destino de NPC do lobby", error);
            unavailable(player);
        }
    }

    private static void unavailable(ServerPlayer player) {
        player.displayClientMessage(Component.literal("§cEste destino está temporariamente indisponível."), false);
    }
}
