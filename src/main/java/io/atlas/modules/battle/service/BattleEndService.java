package io.atlas.modules.battle.service;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.battles.BattleRegistry;
import net.minecraft.server.level.ServerPlayer;

public class BattleEndService {
    private final BattleMusicService musicService;

    public BattleEndService(BattleMusicService musicService) {
        this.musicService = musicService;
    }

    public boolean endBattle(ServerPlayer player) {
        PokemonBattle battle = BattleRegistry.getBattleByParticipatingPlayer(player);
        if (battle == null) {
            return false;
        }

        var participants = java.util.List.copyOf(battle.getPlayers());
        try {
            // Cobblemon end() already invokes closeBattle and its end handlers.
            battle.end();
        } finally {
            participants.forEach(musicService::stopBattleTheme);
        }
        return true;
    }
}
