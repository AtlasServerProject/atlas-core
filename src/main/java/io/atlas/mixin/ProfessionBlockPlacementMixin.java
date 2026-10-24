package io.atlas.mixin;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockItem.class)
public class ProfessionBlockPlacementMixin {
    @Inject(method="place",at=@At("RETURN"))
    private void atlas$placed(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> cir){
        if(cir.getReturnValue().consumesAction()&&context.getLevel() instanceof ServerLevel world&&context.getPlayer() instanceof ServerPlayer player)
            io.atlas.modules.engagement.EngagementModule.service().placed(player,world,context.getClickedPos(),world.getBlockState(context.getClickedPos()));
    }
}
