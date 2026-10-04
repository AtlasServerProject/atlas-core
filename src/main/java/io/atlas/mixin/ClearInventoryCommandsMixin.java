package io.atlas.mixin;

import io.atlas.modules.rank.RankModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ClearInventoryCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import java.util.function.Predicate;

@Mixin(ClearInventoryCommands.class)
public class ClearInventoryCommandsMixin {
    @ModifyArg(method = "register", at = @At(value = "INVOKE", target = "Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;requires(Ljava/util/function/Predicate;)Lcom/mojang/brigadier/builder/ArgumentBuilder;"), index = 0)
    private static Predicate<CommandSourceStack> atlas$restrictClear(Predicate<CommandSourceStack> original) {
        return source -> source.getPlayer() != null
                ? RankModule.getRankService().canManageRanks(source.getPlayer().getUUID())
                : original.test(source);
    }
}
