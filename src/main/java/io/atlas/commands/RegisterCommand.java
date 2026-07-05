package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.auth.service.AuthService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class RegisterCommand {

    private static final AuthService authService = AuthModule.getAuthService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("register")
                        .requires(CommandSourceStack::isPlayer)
                        .then(Commands.argument("senha", StringArgumentType.word())
                                .then(Commands.argument("confirmacao", StringArgumentType.word())
                                        .executes(context -> execute(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "senha"),
                                                StringArgumentType.getString(context, "confirmacao")
                                        ))))
        );
    }

    private static int execute(
            CommandSourceStack source,
            String password,
            String confirmation
    ) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        AuthService.RegistrationResult result = authService.register(
                player.getUUID(),
                password,
                confirmation,
                player.getIpAddress()
        );

        Component message = switch (result) {
            case SUCCESS -> Component.literal("§aRegistro concluído. Você está autenticado.");
            case ALREADY_REGISTERED -> Component.literal("§cEsta conta já está registrada. Use /login.");
            case PASSWORD_MISMATCH -> Component.literal("§cAs senhas informadas não são iguais.");
            case INVALID_PASSWORD -> Component.literal(
                    "§cA senha deve possuir ao menos 8 caracteres e no máximo 72 bytes."
            );
            case SESSION_NOT_FOUND -> Component.literal("§cSua sessão ainda não foi carregada.");
        };

        if (result == AuthService.RegistrationResult.SUCCESS) {
            source.sendSuccess(() -> message, false);
            return 1;
        }

        source.sendFailure(message);
        return 0;
    }
}
