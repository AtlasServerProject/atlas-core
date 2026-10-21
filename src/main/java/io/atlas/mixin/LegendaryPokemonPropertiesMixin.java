package io.atlas.mixin;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import io.atlas.modules.legendary.service.LegendaryIvService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=PokemonProperties.class,remap=false)
public class LegendaryPokemonPropertiesMixin {
 @Inject(method="create",at=@At("RETURN"),remap=false)
 private void atlas$created(CallbackInfoReturnable<Pokemon> callback){LegendaryIvService.ensure(callback.getReturnValue());}
 @Inject(method="apply(Lcom/cobblemon/mod/common/pokemon/Pokemon;)V",at=@At("TAIL"),remap=false)
 private void atlas$applied(Pokemon pokemon,CallbackInfo callback){LegendaryIvService.ensure(pokemon);}
}
