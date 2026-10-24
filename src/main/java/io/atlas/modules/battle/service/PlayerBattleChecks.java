package io.atlas.modules.battle.service;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.battles.BattleRegistry;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rules.RulesModule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
public final class PlayerBattleChecks {
 public static String error(MinecraftServer server,ServerPlayer first,ServerPlayer second){
        if (first.getUUID().equals(second.getUUID())) return "Escolha dois jogadores diferentes.";
        for (ServerPlayer player : new ServerPlayer[]{first, second}) {
            String name = player.getName().getString();
            if (server.getPlayerList().getPlayer(player.getUUID()) != player)
                return name + " não está online.";
            if (!AuthModule.getAuthService().isAuthenticated(player.getUUID()) || RulesModule.service().pending(player.getUUID()))
                return name + " precisa concluir o login e aceitar as regras.";
            if (!player.isAlive() || player.isSpectator() || player.isSleeping() || player.isPassenger())
                return name + " não está disponível para batalhar agora.";
            if (BattleRegistry.getBattleByParticipatingPlayer(player) != null)
                return name + " já está em uma batalha.";
            boolean ready = false;
            for (var pokemon : Cobblemon.INSTANCE.getStorage().getParty(player))
                if (pokemon != null && pokemon.getCurrentHealth() > 0) { ready = true; break; }
            if (!ready) return name + " precisa de pelo menos um Pokémon com HP na equipe.";
        }
        if (first.level() != second.level()) return "Os dois jogadores precisam estar no mesmo mundo.";
        if (first.distanceToSqr(second) > 64 * 64) return "Aproxime os jogadores: distância máxima de 64 blocos.";
        return null;
 }
 private PlayerBattleChecks(){}
}
