package io.atlas.modules.lobby.service;

import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.starter.StarterChosenEvent;
import com.cobblemon.mod.common.api.reactive.ObservableSubscription;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class LobbyStarterProtectionService {

    private ObservableSubscription<StarterChosenEvent> starterSubscription;

    public void enable() {
        starterSubscription = CobblemonEvents.STARTER_CHOSEN.subscribe(event -> {
            ServerPlayer player = event.getPlayer();
            if (player.level().dimension() != Level.OVERWORLD) {
                return;
            }

            event.cancel();
            player.displayClientMessage(
                    Component.literal(
                            "§cA escolha do Pokémon inicial não está disponível no Auth Lobby. "
                                    + "§eEscolha um servidor pela bússola para continuar."
                    ),
                    false
            );
        });
    }

    public void disable() {
        if (starterSubscription != null) {
            starterSubscription.unsubscribe();
            starterSubscription = null;
        }
    }
}
