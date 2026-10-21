package io.atlas.modules.lobby.service;

import io.atlas.AtlasMod;
import io.atlas.modules.auth.service.AuthService;
import io.atlas.modules.economy.CobbleDollarsEconomyBridge;
import io.atlas.modules.economy.EconomyService;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.BlankFormat;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Per-connection HUD. Never modifies the global scoreboard or rank teams. */
public final class LobbyHudService {
    private static final String OBJECTIVE = "atlas_hud";
    private static final String[] TIPS = {"/kits • Seus kits", "/homes • Suas homes", "/compras • Correio de compras"};
    private final AuthService auth;
    private final RankService ranks;
    private final CobbleDollarsEconomyBridge economy = new CobbleDollarsEconomyBridge();
    private final Map<UUID, View> views = new HashMap<>();
    private int ticks;
    private int lastWarningTick = -1200;

    public LobbyHudService(AuthService auth, RankService ranks) {
        this.auth = auth;
        this.ranks = ranks;
    }

    public void tick(MinecraftServer server) {
        ticks++;
        // Remove HUD immediately on authentication/world changes; refresh values once per second.
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean eligible = auth.isAuthenticated(player.getUUID())
                    && (LobbyWorlds.isEmerald(player.level()) || LobbyWorlds.isSurvivalArea(player.level()));
            if (!eligible) {
                remove(player, true);
                continue;
            }
            if (ticks % 20 != 0 && views.containsKey(player.getUUID())) continue;
            try {
                update(player, server.getPlayerList().getPlayerCount());
            } catch (RuntimeException exception) {
                remove(player, true);
                if (ticks - lastWarningTick >= 1200) {
                    lastWarningTick = ticks;
                    AtlasMod.LOGGER.warn("HUD Atlas temporariamente indisponível; nova tentativa no próximo ciclo.");
                }
            }
        }
    }

    private void update(ServerPlayer player, int online) {
        String world = LobbyWorlds.isEmerald(player.level()) ? "Lobby Emerald"
                : LobbyWorlds.isSurvivalEmerald(player.level()) ? "Survival Emerald"
                : player.level().dimension().equals(net.minecraft.world.level.Level.NETHER) ? "Nether" : "The End";
        String rank = ranks.getHighestRank(player.getUUID()).map(r -> r.getDisplayName()).orElse("Player");
        String balance = economy.isAvailable(player) ? EconomyService.format(economy.getBalance(player)) : "Indisponível";
        List<Component> rows = List.of(
                Component.literal("Jogador: ").withStyle(ChatFormatting.GRAY).append(Component.literal(player.getGameProfile().getName()).withStyle(ChatFormatting.WHITE)),
                Component.literal("Cargo: ").withStyle(ChatFormatting.GRAY).append(Component.literal(rank).withStyle(ChatFormatting.AQUA)),
                Component.literal("Saldo: ").withStyle(ChatFormatting.GRAY).append(Component.literal(balance).withStyle(ChatFormatting.GOLD)),
                Component.literal("Online: ").withStyle(ChatFormatting.GRAY).append(Component.literal(Integer.toString(online)).withStyle(ChatFormatting.GREEN)),
                Component.literal("Mundo: ").withStyle(ChatFormatting.GRAY).append(Component.literal(world).withStyle(ChatFormatting.WHITE)));
        View view = views.get(player.getUUID());
        if (view == null) {
            view = new View(player);
            views.put(player.getUUID(), view);
            player.connection.send(new ClientboundSetObjectivePacket(view.objective, ClientboundSetObjectivePacket.METHOD_ADD));
            player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR, view.objective));
            view.bar.addPlayer(player);
        }
        for (int i = 0; i < rows.size(); i++) {
            if (view.rows == null || !rows.get(i).equals(view.rows.get(i))) {
                player.connection.send(new ClientboundSetScorePacket("atlas_row_" + i, OBJECTIVE, rows.size() - i,
                        Optional.of(rows.get(i)), Optional.of(BlankFormat.INSTANCE)));
            }
        }
        view.rows = rows;
        view.bar.setName(Component.literal("ATLAS • " + world + " • " + TIPS[(ticks / 200) % TIPS.length])
                .withStyle(ChatFormatting.AQUA));
    }

    public void disconnect(ServerPlayer player) {
        remove(player, false);
    }

    private void remove(ServerPlayer player, boolean connected) {
        View view = views.remove(player.getUUID());
        if (view == null) return;
        view.bar.removeAllPlayers();
        if (connected) {
            // Restore whatever the shared scoreboard currently displays, preserving other systems.
            player.connection.send(new ClientboundSetDisplayObjectivePacket(DisplaySlot.SIDEBAR,
                    player.getServer().getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR)));
            player.connection.send(new ClientboundSetObjectivePacket(view.objective, ClientboundSetObjectivePacket.METHOD_REMOVE));
        }
    }

    public void clear() {
        for (View view : List.copyOf(views.values())) remove(view.player, true);
        views.clear();
    }

    private static final class View {
        private final ServerPlayer player;
        private final Objective objective;
        private final ServerBossEvent bar;
        private List<Component> rows;

        private View(ServerPlayer player) {
            this.player = player;
            objective = new Scoreboard().addObjective(OBJECTIVE, ObjectiveCriteria.DUMMY,
                    Component.literal("ATLAS COBBLEMON").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    ObjectiveCriteria.RenderType.INTEGER, false, BlankFormat.INSTANCE);
            bar = new ServerBossEvent(Component.literal("ATLAS • Emerald"), BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.PROGRESS);
            bar.setProgress(1.0F);
        }
    }
}
