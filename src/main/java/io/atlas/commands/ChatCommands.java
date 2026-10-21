package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import io.atlas.modules.chat.ChatModule;
public final class ChatCommands {
 public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
  var service=ChatModule.service();
  var nodes=(io.atlas.mixin.CommandNodeAccessor)(Object)dispatcher.getRoot();
  for(String alias:new String[]{"tell","msg","w"}){nodes.atlas$children().remove(alias);nodes.atlas$literals().remove(alias);}

  for(String channel:new String[]{"global","local","feira","vip","staff"}){
   var root=Commands.literal(channel).requires(CommandSourceStack::isPlayer);
   if(!channel.equals("feira"))root.executes(c->service.select(c.getSource().getPlayerOrException(),channel));
   root.then(Commands.argument("mensagem",StringArgumentType.greedyString()).executes(c->service.send(c.getSource().getPlayerOrException(),channel,StringArgumentType.getString(c,"mensagem"))));
   dispatcher.register(root);
  }
  for(String alias:new String[]{"tell","msg","w"})dispatcher.register(Commands.literal(alias).requires(CommandSourceStack::isPlayer)
   .then(Commands.argument("jogador",EntityArgument.player()).then(Commands.argument("mensagem",StringArgumentType.greedyString())
    .executes(c->service.tell(c.getSource().getPlayerOrException(),EntityArgument.getPlayer(c,"jogador"),StringArgumentType.getString(c,"mensagem"))))));
 }
}
