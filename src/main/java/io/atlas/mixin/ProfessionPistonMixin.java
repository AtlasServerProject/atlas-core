package io.atlas.mixin;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.Level;
import net.minecraft.core.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(PistonBaseBlock.class)
public class ProfessionPistonMixin {
    @Inject(method="moveBlocks",at=@At("HEAD"))
    private void atlas$moved(Level level,BlockPos pos,Direction direction,boolean extend,CallbackInfoReturnable<Boolean> cir){
        io.atlas.modules.engagement.EngagementModule.service().piston(level,pos,direction,extend);
    }
}
