package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.legendary.LegendaryModule;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rank.RankModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
public final class LegendaryCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("lendarios").requires(s->!s.isPlayer()||AuthModule.getAuthService().isAuthenticated(s.getPlayer().getUUID())&&RankModule.getRankService().canManageRanks(s.getPlayer().getUUID()))
                .executes(c->LegendaryModule.service().command(c.getSource(),"next",null)));
        var root=Commands.literal("atlaslegendary").requires(s->!s.isPlayer()||AuthModule.getAuthService().isAuthenticated(s.getPlayer().getUUID())&&RankModule.getRankService().canManageRanks(s.getPlayer().getUUID()))
                .executes(c->LegendaryModule.service().command(c.getSource(),"status",null));
        for(String action:new String[]{"next","force","reload","enable","disable","status","list","active"})root.then(Commands.literal(action).executes(c->LegendaryModule.service().command(c.getSource(),action,null)));
        root.then(Commands.literal("test").then(Commands.argument("species",StringArgumentType.word()).executes(c->LegendaryModule.service().command(c.getSource(),"test",StringArgumentType.getString(c,"species")))));
        dispatcher.register(root);
        dispatcher.register(Commands.literal("spawnlegendary")
            .requires(s->!s.isPlayer()||AuthModule.getAuthService().isAuthenticated(s.getPlayer().getUUID())&&RankModule.getRankService().canManageRanks(s.getPlayer().getUUID()))
            .executes(c->LegendaryModule.service().command(c.getSource(),"force",null))
            .then(Commands.argument("species",StringArgumentType.word()).executes(c->LegendaryModule.service().command(c.getSource(),"test",StringArgumentType.getString(c,"species")))));
    }
    /** Extend Cobblemon's existing command without replacing its root or normal arguments. */
    public static void attachPreview(CommandDispatcher<CommandSourceStack> dispatcher){
        var root=dispatcher.getRoot().getChild("checkspawns");
        if(root==null){root=dispatcher.register(Commands.literal("checkspawns").requires(source->source.hasPermission(2)));}
        root.addChild(Commands.literal("legendary")
            .requires(source->source.isPlayer()&&AuthModule.getAuthService().isAuthenticated(source.getPlayer().getUUID())&&(RankModule.getRankService().canManageRanks(source.getPlayer().getUUID())||io.atlas.modules.pokedex.PokedexModule.service().professor(source.getPlayer().getUUID())))
            .executes(context->LegendaryModule.service().preview(context.getSource())).build());
    }
}
