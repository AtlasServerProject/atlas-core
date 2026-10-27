package io.atlas.mixin;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.*;
@Mixin(RecipeManager.class)
public class RestrictedRecipeMixin {
 @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private net.minecraft.core.HolderLookup.Provider registries;
 private static boolean atlas$cap(ItemStack s){return !s.isEmpty()&&io.atlas.modules.shopkeeper.model.ShopRestrictions.bottleCap(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());}
 @Inject(method={"getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"},at=@At("HEAD"),cancellable=true)
 private <I extends RecipeInput,T extends Recipe<I>> void atlas$input(RecipeType<T> type,I input,Level level,CallbackInfoReturnable<Optional<RecipeHolder<T>>> cir){for(int i=0;i<input.size();i++)if(atlas$cap(input.getItem(i))){cir.setReturnValue(Optional.empty());return;}}
 @Inject(method="apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",at=@At("TAIL"))
 private void atlas$filter(Map<?,?> data,net.minecraft.server.packs.resources.ResourceManager resources,net.minecraft.util.profiling.ProfilerFiller profiler,CallbackInfo ci){
  var manager=(RecipeManager)(Object)this;
  var safe=manager.getRecipes().stream().filter(h->!atlas$cap(h.value().getResultItem(registries))).filter(h->h.value().getIngredients().stream().noneMatch(i->Arrays.stream(i.getItems()).anyMatch(RestrictedRecipeMixin::atlas$cap))).toList();
  int removed=manager.getRecipes().size()-safe.size();manager.replaceRecipes(safe);
  io.atlas.AtlasMod.LOGGER.info("Atlas: {} receitas com Bottle Caps removidas",removed);
 }
}
