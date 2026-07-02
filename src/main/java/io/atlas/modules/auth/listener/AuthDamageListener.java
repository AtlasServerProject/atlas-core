package io.atlas.modules.auth.listener;

import io.atlas.modules.auth.service.AuthDamageProtectionService;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

public class AuthDamageListener {

    public static void register(AuthDamageProtectionService protectionService) {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof ServerPlayer player)
                        || protectionService.canTakeDamage(player, source)
        );
    }
}
