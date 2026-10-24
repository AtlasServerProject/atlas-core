package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import io.atlas.modules.engagement.EngagementModule;
import io.atlas.modules.auth.AuthModule;
public final class EngagementCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        for(String name:java.util.List.of("diario","profissoes")){boolean daily=name.equals("diario");
            dispatcher.register(Commands.literal(name).requires(s->s.isPlayer()&&AuthModule.getAuthService().isAuthenticated(s.getPlayer().getUUID()))
                .executes(c->EngagementModule.service().open(c.getSource().getPlayerOrException(),daily))
                .then(Commands.literal("resgatar").executes(c->{EngagementModule.service().claim(c.getSource().getPlayerOrException(),daily);return 1;})));
        }
    }
}
