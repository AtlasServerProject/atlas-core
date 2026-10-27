package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import io.atlas.modules.shopkeeper.ShopkeeperModule;
public final class ShopkeeperCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("lojistas").requires(s->!s.isPlayer() ||
            io.atlas.modules.auth.AuthModule.getAuthService().isAuthenticated(s.getPlayer().getUUID()) &&
            io.atlas.modules.rank.RankModule.getRankService().canManageRanks(s.getPlayer().getUUID()))
            .executes(c->ShopkeeperModule.service().diagnose(c.getSource()))
            .then(Commands.literal("atualizar").executes(c->{ShopkeeperModule.service().reload(c.getSource());return 1;})));
    }
}
