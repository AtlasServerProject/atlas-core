package io.atlas.modules.battle.service;

import com.cobblemon.mod.common.api.battles.model.PokemonBattle;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.battles.BattleFledEvent;
import com.cobblemon.mod.common.api.events.battles.BattleStartedEvent;
import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import io.atlas.modules.lobby.service.WorldThemeService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BattleMusicService {

    private static final SoundEvent DEFAULT_BATTLE_THEME = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath("cobblesounds", "battle_regis_hoenn")
    );

    private final WorldThemeService worldThemeService;
    private final Set<UUID> playersInBattle = new HashSet<>();
    private ObservableSubscription<BattleStartedEvent.Post> battleStartedSubscription;
    private ObservableSubscription<BattleVictoryEvent> battleVictorySubscription;
    private ObservableSubscription<BattleFledEvent> battleFledSubscription;

    public BattleMusicService(WorldThemeService worldThemeService) {
        this.worldThemeService = worldThemeService;
    }

    public void enable() {
        battleStartedSubscription = CobblemonEvents.BATTLE_STARTED_POST.subscribe(event ->
                startBattleTheme(event.getBattle())
        );
        battleVictorySubscription = CobblemonEvents.BATTLE_VICTORY.subscribe(event ->
                stopBattleTheme(event.getBattle())
        );
        battleFledSubscription = CobblemonEvents.BATTLE_FLED.subscribe(event ->
                stopBattleTheme(event.getBattle())
        );
    }

    public void disable() {
        if (battleStartedSubscription != null) {
            battleStartedSubscription.unsubscribe();
            battleStartedSubscription = null;
        }
        if (battleVictorySubscription != null) {
            battleVictorySubscription.unsubscribe();
            battleVictorySubscription = null;
        }
        if (battleFledSubscription != null) {
            battleFledSubscription.unsubscribe();
            battleFledSubscription = null;
        }
        playersInBattle.clear();
    }

    public void startBattleTheme(PokemonBattle battle) {
        for (ServerPlayer player : battle.getPlayers()) {
            playersInBattle.add(player.getUUID());
            worldThemeService.playPriority(player, DEFAULT_BATTLE_THEME);
        }
    }

    public void stopBattleTheme(PokemonBattle battle) {
        for (ServerPlayer player : battle.getPlayers()) {
            stopBattleTheme(player);
        }
    }

    public void stopBattleTheme(ServerPlayer player) {
        if (!playersInBattle.remove(player.getUUID())) {
            return;
        }
        worldThemeService.clearPriority(player);
    }
}
