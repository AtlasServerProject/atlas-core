package io.atlas.mixin;
import com.cobblemon.mod.common.api.PrioritizedList;
import com.cobblemon.mod.common.api.storage.PokemonStoreManager;
import com.cobblemon.mod.common.api.storage.factory.PokemonStoreFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=PokemonStoreManager.class,remap=false)
public interface PokemonStoreManagerAccessor {
 @Accessor("factories") PrioritizedList<PokemonStoreFactory> atlas$factories();
}
