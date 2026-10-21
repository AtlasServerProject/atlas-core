package io.atlas.mixin;
import com.cobblemon.mod.common.api.storage.factory.FileBackedPokemonStoreFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.concurrent.ExecutorService;
@Mixin(value=FileBackedPokemonStoreFactory.class,remap=false)
public interface PokemonFileStoreAccessor {
 @Accessor("adapter") com.cobblemon.mod.common.api.storage.adapter.flatfile.FileStoreAdapter<?> atlas$adapter();
 @Accessor("saveExecutor") ExecutorService atlas$saveExecutor();
}
