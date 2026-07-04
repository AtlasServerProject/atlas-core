package io.atlas.mixin;

import io.atlas.modules.claim.ClaimModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public class ClaimFluidMixin {
    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void atlas$protectClaims(LevelAccessor level, BlockPos pos, BlockState state,
                                     Direction direction, FluidState fluid, CallbackInfo ci) {
        if (level instanceof net.minecraft.world.level.Level world
                && ClaimModule.getService().isClaimed(world, pos)) ci.cancel();
    }
}
