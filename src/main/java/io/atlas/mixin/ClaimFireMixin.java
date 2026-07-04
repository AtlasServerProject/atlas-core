package io.atlas.mixin;

import io.atlas.modules.claim.ClaimModule;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class ClaimFireMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void atlas$stopFire(BlockState state, ServerLevel level, BlockPos pos,
                                RandomSource random, CallbackInfo ci) {
        if (ClaimModule.getService().isClaimed(level, pos)) {
            level.removeBlock(pos, false);
            ci.cancel();
        }
    }

    @Inject(method = "checkBurnOut", at = @At("HEAD"), cancellable = true)
    private void atlas$stopBurn(Level level, BlockPos pos, int chance, RandomSource random,
                                int age, CallbackInfo ci) {
        if (ClaimModule.getService().isClaimed(level, pos)) ci.cancel();
    }
}
