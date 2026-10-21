package io.atlas.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Atlas owns /pc so its session link uses authenticated VIP/staff access on every server. */
@Mixin(targets = "com.cobblemon.mod.common.command.PcCommand", remap = false)
public class CobblemonPcCommandMixin {
    @Inject(method = "register", at = @At("HEAD"), cancellable = true, remap = false)
    private void atlas$useAtlasPcCommand(CallbackInfo callback) {
        callback.cancel();
    }
}
