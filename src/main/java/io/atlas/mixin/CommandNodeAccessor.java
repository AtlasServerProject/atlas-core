package io.atlas.mixin;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=CommandNode.class,remap=false)
public interface CommandNodeAccessor {
 @Accessor("children") Map<String,?> atlas$children();
 @Accessor("literals") Map<String,?> atlas$literals();
}
