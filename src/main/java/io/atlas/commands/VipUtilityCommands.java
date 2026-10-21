package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import io.atlas.modules.vipcommands.service.VipCommandsService;
import net.minecraft.commands.*;
public final class VipUtilityCommands {
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
  for(String command:new String[]{"craft","trash","repair","pc","pokeheal","hatch","hat","lojas"})dispatcher.register(Commands.literal(command).requires(s->s.isPlayer()&&(VipCommandsService.canUse(s.getPlayer(),1)||((command.equals("pc")||command.equals("pokeheal"))&&io.atlas.modules.pokedex.PokedexModule.service().professor(s.getPlayer().getUUID())))).executes(c->VipCommandsService.use(c.getSource().getPlayerOrException(),command)));
  dispatcher.register(Commands.literal("warp").requires(CommandSourceStack::isPlayer).then(Commands.literal("vip").requires(s->VipCommandsService.canUse(s.getPlayer(),1)).executes(c->VipCommandsService.travel(c.getSource().getPlayerOrException(),"vip"))));
  for(String name:new String[]{"vip","lojas"})dispatcher.register(Commands.literal("setwarp").requires(s->s.isPlayer()&&io.atlas.modules.rank.RankModule.getRankService().canManageRanks(s.getPlayer().getUUID())).then(Commands.literal(name).executes(c->VipCommandsService.setWarp(c.getSource().getPlayerOrException(),name))));
  for(String command:new String[]{"nick","setcolor"})dispatcher.register(Commands.literal(command).requires(s->s.isPlayer()&&VipCommandsService.canUse(s.getPlayer(),1)).then(Commands.argument("valor",StringArgumentType.word()).executes(c->VipCommandsService.preference(c.getSource().getPlayerOrException(),command.equals("nick")?"nickname":"chat_color",StringArgumentType.getString(c,"valor")))));
  dispatcher.register(Commands.literal("pokecolor").requires(s->s.isPlayer()&&VipCommandsService.canUse(s.getPlayer(),1)).then(Commands.argument("slot",IntegerArgumentType.integer(1,6)).then(Commands.argument("cor",StringArgumentType.word()).executes(c->VipCommandsService.pokeColor(c.getSource().getPlayerOrException(),IntegerArgumentType.getInteger(c,"slot"),StringArgumentType.getString(c,"cor"))))));
  dispatcher.register(Commands.literal("checkspawns").requires(s->s.isPlayer()&&(VipCommandsService.canUse(s.getPlayer(),3)||io.atlas.modules.auth.AuthModule.getAuthService().isAuthenticated(s.getPlayer().getUUID())&&io.atlas.modules.pokedex.PokedexModule.service().professor(s.getPlayer().getUUID()))).executes(c->VipCommandsService.spawns(c.getSource().getPlayerOrException(),"common")).then(Commands.argument("grupo",StringArgumentType.word()).executes(c->VipCommandsService.spawns(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"grupo")))));
 }
}
