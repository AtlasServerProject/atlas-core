package io.atlas.mixin;

import io.atlas.modules.claim.ClaimModule;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Explosion.class)
public abstract class ClaimExplosionMixin {
    @Shadow @Final private Level level;
    @Shadow public abstract java.util.List<net.minecraft.core.BlockPos> getToBlow();

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void atlas$protectClaimBlocks(boolean particles, CallbackInfo ci) {
        getToBlow().removeIf(pos -> ClaimModule.getService().isClaimed(level, pos));
    }
}
