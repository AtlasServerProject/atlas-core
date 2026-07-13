package io.atlas.modules.battle;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.battle.service.BattleEndService;
import io.atlas.modules.battle.service.BattleMusicService;
import io.atlas.modules.survival.SurvivalModule;

public class BattleModule implements AtlasModule {

    private static final BattleEndService BATTLE_END_SERVICE = new BattleEndService();
    private static final BattleMusicService BATTLE_MUSIC_SERVICE =
            new BattleMusicService(SurvivalModule.getWorldThemeService());

    public static BattleEndService getBattleEndService() {
        return BATTLE_END_SERVICE;
    }

    public static BattleMusicService getBattleMusicService() {
        return BATTLE_MUSIC_SERVICE;
    }

    @Override
    public String getName() {
        return "Battle";
    }

    @Override
    public void enable() {
        BATTLE_MUSIC_SERVICE.enable();
        AtlasMod.LOGGER.info("Serviço de batalhas do Atlas iniciado.");
    }

    @Override
    public void disable() {
        BATTLE_MUSIC_SERVICE.disable();
    }
}
