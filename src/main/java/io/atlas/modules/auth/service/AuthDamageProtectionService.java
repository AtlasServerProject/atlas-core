package io.atlas.modules.auth.service;

import io.atlas.modules.lobby.service.LobbyWorlds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

public class AuthDamageProtectionService {

    private final AuthService authService;

    public AuthDamageProtectionService(AuthService authService) {
        this.authService = authService;
    }

    public boolean canTakeDamage(ServerPlayer player, DamageSource source) {
        if (!authService.isAuthenticated(player.getUUID())) {
            return false;
        }

        return !isLobby(player) || !source.is(DamageTypes.FALL);
    }

    private boolean isLobby(ServerPlayer player) {
        return LobbyWorlds.isLobby(player.level());
    }
}
