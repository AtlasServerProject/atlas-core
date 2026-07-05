package io.atlas.modules.battle.service;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleRegistry;
import net.minecraft.server.level.ServerPlayer;

public class BattleEndService {

    public boolean endBattle(ServerPlayer player) {
        PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
        if (battle == null) {
            return false;
        }

        battle.end();
        BattleRegistry.closeBattle(battle);
        return true;
    }
}
