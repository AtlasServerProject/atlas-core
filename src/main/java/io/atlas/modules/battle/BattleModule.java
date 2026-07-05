package io.atlas.modules.battle;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.battle.service.BattleEndService;

public class BattleModule implements AtlasModule {

    private static final BattleEndService BATTLE_END_SERVICE = new BattleEndService();

    public static BattleEndService getBattleEndService() {
        return BATTLE_END_SERVICE;
    }

    @Override
    public String getName() {
        return "Battle";
    }

    @Override
    public void enable() {
        AtlasMod.LOGGER.info("Serviço de batalhas do Atlas iniciado.");
    }

    @Override
    public void disable() {
    }
}
