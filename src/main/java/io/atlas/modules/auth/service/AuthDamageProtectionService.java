package io.atlas.modules.auth.service;

import io.atlas.modules.lobby.service.LobbyWorlds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;

public class AuthDamageProtectionService {

    private final AuthService authService;

    public AuthDamageProtectionService(AuthService authService) {
        this.authService = authService;
    }

    public boolean canTakeDamage(ServerPlayer player, DamageSource source) {
        if (!authService.isAuthenticated(player.getUUID())) {
            return false;
        }

        if (isPlayerVersusPlayer(source)) {
            return false;
        }

        if (isFallDamageDisabled(player, source)) {
            return false;
        }

        return true;
    }

    private boolean isLobby(ServerPlayer player) {
        return LobbyWorlds.isLobby(player.level());
    }

    private boolean isFallDamageDisabled(ServerPlayer player, DamageSource source) {
        return source.is(DamageTypes.FALL)
                && (isLobby(player) || LobbyWorlds.isSurvivalArea(player.level()));
    }

    private boolean isPlayerVersusPlayer(DamageSource source) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        return attacker instanceof ServerPlayer || direct instanceof ServerPlayer;
    }
}
