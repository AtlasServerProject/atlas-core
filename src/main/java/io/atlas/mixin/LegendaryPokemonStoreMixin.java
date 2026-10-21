package io.atlas.mixin;
import com.cobblemon.mod.common.api.storage.*;
import com.cobblemon.mod.common.pokemon.Pokemon;
import io.atlas.modules.legendary.service.LegendaryIvService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=PokemonStore.class,remap=false)
public class LegendaryPokemonStoreMixin {
 @Inject(method="set",at=@At("HEAD"),remap=false)
 private void atlas$stored(StorePosition position,Pokemon pokemon,CallbackInfo callback){LegendaryIvService.ensure(pokemon);}
}
