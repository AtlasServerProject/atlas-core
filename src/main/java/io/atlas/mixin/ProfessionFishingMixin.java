package io.atlas.mixin;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(FishingHook.class)
public class ProfessionFishingMixin {
    @Shadow private int nibble;
    @Unique private boolean atlas$successful;
    @Inject(method="retrieve",at=@At("HEAD"))
    private void atlas$before(ItemStack rod,CallbackInfoReturnable<Integer> cir){atlas$successful=nibble>0;}
    @Inject(method="retrieve",at=@At("RETURN"))
    private void atlas$caught(ItemStack rod,CallbackInfoReturnable<Integer> cir){
        if(!((Object)this instanceof com.cobblemon.mod.common.entity.fishing.PokeRodFishingBobberEntity)&&atlas$successful&&cir.getReturnValue()>0&&((FishingHook)(Object)this).getPlayerOwner() instanceof ServerPlayer player)
            io.atlas.modules.engagement.EngagementModule.service().fished(player);
        atlas$successful=false;
    }
}
