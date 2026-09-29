package io.atlas.modules.moderation.service;

import com.cobblemon.mod.common.battles.BattleRegistry;
import io.atlas.AtlasMod;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.home.HomeModule;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.moderation.ModerationModule;
import io.atlas.modules.moderation.model.StaffModeSession;
import io.atlas.modules.moderation.repository.StaffModeRepository;
import io.atlas.modules.rank.RankModule;
import io.atlas.modules.survival.SurvivalModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StaffModeService {
    private final StaffModeRepository repository = new StaffModeRepository(DatabaseManager::getConnection);
    private final Map<UUID, StaffModeSession> active = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> skipPositionSave = ConcurrentHashMap.newKeySet();
    private int ticks;

    public boolean skipPositionSave(UUID uuid) { return isActive(uuid) || skipPositionSave.contains(uuid); }
    public boolean isActive(UUID uuid) { return active.containsKey(uuid); }
    public boolean blocksCommand(ServerPlayer player, String command) {
        return isActive(player.getUUID()) && !StaffModePolicy.allows(command);
    }
    public boolean canUse(CommandSourceStack source) {
        var player = source.getPlayer();
        return player != null && (isActive(player.getUUID()) || authorized(player));
    }
    private boolean authorized(ServerPlayer player) {
        return AuthModule.getAuthService().isAuthenticated(player.getUUID())
                && RankModule.getRankService().isStaff(player.getUUID());
    }

    public int toggle(CommandSourceStack source, Boolean enable) {
        ServerPlayer player = source.getPlayer();
        if (player == null) return 0;
        boolean on = isActive(player.getUUID());
        boolean requested = enable == null ? !on : enable;
        try {
            if (requested == on) {
                source.sendSuccess(() -> Component.literal(on ? "§eStaffMode já está ativo." : "§eStaffMode já está desativado."), false);
                return 1;
            }
            if (!requested) {
                finish(player, !LobbyWorlds.isAuth(player.level()));
                source.sendSuccess(() -> Component.literal("§aStaffMode desativado. Estado anterior restaurado."), false);
                return 1;
            }
            if (!authorized(player) || LobbyWorlds.isAuth(player.level()) || ModerationModule.getStaffTools().isFrozen(player.getUUID())) {
                source.sendFailure(Component.literal("§cApenas staff autenticada, fora do Auth Hub e sem freeze pode ativar o StaffMode."));
                return 0;
            }
            if (player.isPassenger() || player.isSleeping() || BattleRegistry.getBattleByParticipatingPlayer(player) != null
                    || HomeModule.getHomeService().hasPendingTeleport(player.getUUID())
                    || SurvivalModule.getRandomTeleportService().hasPendingTeleport(player.getUUID())) {
                source.sendFailure(Component.literal("§cTermine a batalha/teleporte e saia da montaria ou cama antes de ativar."));
                return 0;
            }
            var abilities = player.getAbilities();
            var snapshot = new StaffModeSession(player.getUUID(), player.gameMode.getGameModeForPlayer().getName(),
                    player.serverLevel().dimension().location().toString(), player.getX(), player.getY(), player.getZ(),
                    player.getYRot(), player.getXRot(), abilities.mayfly, abilities.flying, abilities.invulnerable,
                    abilities.instabuild, abilities.mayBuild, abilities.getFlyingSpeed(), abilities.getWalkingSpeed());
            if (!repository.begin(snapshot)) {
                recover(player);
                source.sendFailure(Component.literal("§eSessão anterior recuperada. Execute o comando novamente."));
                return 0;
            }
            SurvivalModule.getPositionService().saveIfSurvival(player);
            active.put(player.getUUID(), snapshot); // Only after durable snapshot; never overwrite an unfinished session.
            player.closeContainer();
            player.setGameMode(GameType.SPECTATOR);
            player.setDeltaMovement(Vec3.ZERO);
            player.resetFallDistance();
            AtlasMod.LOGGER.info("[StaffMode] Ativado por {}", player.getUUID());
            source.sendSuccess(() -> Component.literal("§aStaffMode ativado. §7Use /staffmode tp <jogador>, /invsee, /endersee ou /freeze. /staffmode off para sair."), false);
            return 1;
        } catch (RuntimeException e) {
            AtlasMod.LOGGER.error("[StaffMode] Falha para {}", player.getUUID(), e);
            source.sendFailure(Component.literal("§cNão foi possível concluir o StaffMode. O estado de recuperação foi preservado."));
            return 0;
        }
    }

    public int teleport(CommandSourceStack source, ServerPlayer target) {
        var player = source.getPlayer();
        if (player == null || !isActive(player.getUUID()) || !ModerationModule.getStaffTools().canInspect(player, target)) {
            source.sendFailure(Component.literal("§cAtive o StaffMode e escolha um jogador autenticado de cargo inferior, fora do Auth Hub."));
            return 0;
        }
        player.setCamera(player);
        player.teleportTo(target.serverLevel(), target.getX(), target.getY(), target.getZ(), player.getYRot(), player.getXRot());
        AtlasMod.LOGGER.info("[StaffMode] {} teleportou para {}", player.getUUID(), target.getUUID());
        source.sendSuccess(() -> Component.literal("§aTeleportado para " + target.getName().getString() + "."), false);
        return 1;
    }

    private void restore(ServerPlayer player, StaffModeSession snapshot, boolean position, boolean complete) {
        var server = player.getServer();
        var level = server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(snapshot.world())));
        if (position && level == null) throw new IllegalStateException("Mundo original indisponível.");
        player.closeContainer();
        player.setCamera(player);
        player.stopRiding();
        if (position) {
            level.getChunk((int) Math.floor(snapshot.x()) >> 4, (int) Math.floor(snapshot.z()) >> 4);
            player.teleportTo(level, snapshot.x(), snapshot.y(), snapshot.z(), snapshot.yaw(), snapshot.pitch());
        }
        player.setGameMode(GameType.byName(snapshot.gameMode()));
        var a = player.getAbilities();
        a.mayfly = snapshot.mayFly(); a.flying = snapshot.flying(); a.invulnerable = snapshot.invulnerable();
        a.instabuild = snapshot.instantBuild(); a.mayBuild = snapshot.mayBuild();
        a.setFlyingSpeed(snapshot.flySpeed()); a.setWalkingSpeed(snapshot.walkSpeed());
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.onUpdateAbilities();
        // Save restored vanilla state before acknowledging completion in PostgreSQL.
        server.getPlayerList().saveAll();
        if (complete) repository.complete(player.getUUID());
        active.remove(player.getUUID());
        AtlasMod.LOGGER.info("[StaffMode] Estado restaurado para {} (posição: {})", player.getUUID(), position);
    }

    private void finish(ServerPlayer player, boolean position) {
        var snapshot = active.get(player.getUUID());
        if (snapshot != null) restore(player, snapshot, position, true);
    }

    public void recover(ServerPlayer player) {
        try {
            var snapshot = repository.findActive(player.getUUID());
            if (snapshot.isPresent()) {
                // JOIN still follows the authentication lobby; never teleport a reconnecting player past login.
                skipPositionSave.add(player.getUUID());
                restore(player, snapshot.get(), false, true);
                player.sendSystemMessage(Component.literal("§eSua sessão de StaffMode foi encerrada e seu modo anterior restaurado."));
            }
        } catch (RuntimeException e) {
            AtlasMod.LOGGER.error("[StaffMode] Recuperação falhou para {}", player.getUUID(), e);
            player.connection.disconnect(Component.literal("Não foi possível recuperar seu estado de StaffMode. Avise a administração."));
        }
    }

    public boolean finishForFreeze(ServerPlayer player) {
        try { finish(player, true); return true; }
        catch (RuntimeException e) { AtlasMod.LOGGER.error("[StaffMode] Falha antes de freeze", e); return false; }
    }

    public void disconnected(ServerPlayer player) {
        try {
            var snapshot = active.get(player.getUUID());
            if (snapshot != null) {
                skipPositionSave.add(player.getUUID());
                // Keep the durable recovery row until JOIN has saved the restored state.
                restore(player, snapshot, false, false);
            }
        }
        catch (RuntimeException e) { AtlasMod.LOGGER.error("[StaffMode] Recuperação pendente após desconexão de {}", player.getUUID(), e); }
        finally { active.remove(player.getUUID()); }
    }

    public void stopping(MinecraftServer server) {
        for (var player : server.getPlayerList().getPlayers()) {
            try { finish(player, !LobbyWorlds.isAuth(player.level())); }
            catch (RuntimeException e) { AtlasMod.LOGGER.error("[StaffMode] Recuperação persistida para próximo login de {}", player.getUUID(), e); }
        }
    }

    public void tick(MinecraftServer server) {
        ticks++;
        // JOIN and DISCONNECT listeners have finished; inspection positions must not enter the survival cache.
        for (var player : server.getPlayerList().getPlayers()) skipPositionSave.remove(player.getUUID());
        for (var player : server.getPlayerList().getPlayers()) {
            if (!isActive(player.getUUID())) continue;
            if (!authorized(player) || LobbyWorlds.isAuth(player.level())) {
                try {
                    finish(player, !LobbyWorlds.isAuth(player.level()));
                    player.sendSystemMessage(Component.literal("§eStaffMode encerrado por mudança de sessão/permissão."));
                } catch (RuntimeException e) {
                    AtlasMod.LOGGER.error("[StaffMode] Falha ao encerrar modo de {}", player.getUUID(), e);
                    player.connection.disconnect(Component.literal("Falha ao restaurar o StaffMode. Avise a administração."));
                }
                continue;
            }
            if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) player.setGameMode(GameType.SPECTATOR);
            if (ticks % 40 == 0) player.displayClientMessage(Component.literal("§bStaffMode §7• /staffmode off para sair"), true);
        }
    }
}
