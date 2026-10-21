package io.atlas.mixin;
import com.cobblemon.mod.common.pokemon.Pokemon;
import io.atlas.modules.legendary.service.LegendaryIvService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=Pokemon.class,remap=false)
public class LegendaryPokemonLoadMixin {
 @Inject(method={"loadFromNBT","loadFromJSON","initialize"},at=@At("RETURN"),remap=false)
 private void atlas$loaded(CallbackInfoReturnable<Pokemon> callback){LegendaryIvService.ensure(callback.getReturnValue());}
 @Inject(method="setIV",at=@At("TAIL"),remap=false)
 private void atlas$ivChanged(com.cobblemon.mod.common.api.pokemon.stats.Stat stat,int value,CallbackInfo callback){LegendaryIvService.ensure((Pokemon)(Object)this);}
}

