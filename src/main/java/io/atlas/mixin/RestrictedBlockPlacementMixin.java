package io.atlas.mixin;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockItem.class)
public class RestrictedBlockPlacementMixin {
 @Inject(method="place",at=@At("HEAD"),cancellable=true)
 private void atlas$denyRedstone(BlockPlaceContext ctx,CallbackInfoReturnable<InteractionResult> cir){
  if(!ctx.getLevel().isClientSide && io.atlas.modules.shopkeeper.model.ShopRestrictions.redstone(BuiltInRegistries.ITEM.getKey(ctx.getItemInHand().getItem()).toString())){
   if(ctx.getPlayer()!=null)ctx.getPlayer().displayClientMessage(net.minecraft.network.chat.Component.literal("§cComponentes de redstone estão bloqueados no servidor."),true);
   cir.setReturnValue(InteractionResult.FAIL);
  }
 }
}
