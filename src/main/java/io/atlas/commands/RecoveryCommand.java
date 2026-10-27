package io.atlas.commands;
import com.mojang.brigadier.CommandDispatcher;import com.mojang.brigadier.arguments.StringArgumentType;import net.minecraft.commands.*;import io.atlas.modules.auth.AuthModule;
public final class RecoveryCommand {
 public static void register(CommandDispatcher<CommandSourceStack> d){
  d.register(Commands.literal("email").requires(CommandSourceStack::isPlayer)
   .executes(c->{AuthModule.getRecoveryService().notifyMissingEmail(c.getSource().getPlayerOrException());c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("§e/email cadastrar <email> §7e §e/email confirmar <codigo>. §7Faça login primeiro."),false);return 1;})
   .then(Commands.literal("cadastrar").then(Commands.argument("email",StringArgumentType.greedyString()).executes(c->AuthModule.getRecoveryService().email(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"email")))))
   .then(Commands.literal("confirmar").then(Commands.argument("codigo",StringArgumentType.word()).executes(c->AuthModule.getRecoveryService().confirmEmail(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"codigo"))))));
  d.register(Commands.literal("recuperarsenha").requires(CommandSourceStack::isPlayer).executes(c->AuthModule.getRecoveryService().request(c.getSource().getPlayerOrException()))
   .then(Commands.literal("confirmar").then(Commands.argument("codigo",StringArgumentType.word()).then(Commands.argument("novaSenha",StringArgumentType.word()).then(Commands.argument("confirmacao",StringArgumentType.word()).executes(c->AuthModule.getRecoveryService().reset(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"codigo"),StringArgumentType.getString(c,"novaSenha"),StringArgumentType.getString(c,"confirmacao"))))))));
 }
}
