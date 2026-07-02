package io.atlas.modules.auth.service;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.level.Level;

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
        return player.level().dimension() == Level.OVERWORLD;
    }
}
