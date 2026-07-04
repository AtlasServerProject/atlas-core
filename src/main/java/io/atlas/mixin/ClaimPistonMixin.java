package io.atlas.mixin;

import io.atlas.modules.claim.ClaimModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonStructureResolver.class)
public abstract class ClaimPistonMixin {
    @Shadow @Final private Level level;
    @Shadow public abstract java.util.List<BlockPos> getToPush();
    @Shadow public abstract java.util.List<BlockPos> getToDestroy();
    @Shadow public abstract net.minecraft.core.Direction getPushDirection();

    @Inject(method = "resolve", at = @At("RETURN"), cancellable = true)
    private void atlas$protectClaims(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        boolean protectedArea = getToDestroy().stream().anyMatch(pos -> ClaimModule.getService().isClaimed(level, pos))
                || getToPush().stream().anyMatch(pos -> ClaimModule.getService().isClaimed(level, pos)
                || ClaimModule.getService().isClaimed(level, pos.relative(getPushDirection())));
        if (protectedArea) cir.setReturnValue(false);
    }
}
