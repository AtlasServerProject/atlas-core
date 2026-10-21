package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;import net.minecraft.commands.*;
public final class ResearchCommand {
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){for(String name:new String[]{"pesquisas","pesquisa"})dispatcher.register(Commands.literal(name).requires(s->s.isPlayer()&&io.atlas.modules.auth.AuthModule.getAuthService().isAuthenticated(s.getPlayer().getUUID())).executes(c->io.atlas.modules.research.ResearchModule.service().open(c.getSource().getPlayerOrException(),null)));}
}
