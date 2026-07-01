package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.auth.service.AuthService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class LogoutCommand {

    private static final AuthService authService = AuthModule.getAuthService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("logout")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> execute(context.getSource()))
        );
    }

    private static int execute(CommandSourceStack source)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        AuthService.LogoutResult result = authService.logout(player.getUUID());

        return switch (result) {
            case SUCCESS -> {
                source.sendSuccess(
                        () -> Component.literal("§aLogout realizado. Use /login para entrar novamente."),
                        false
                );
                yield 1;
            }
            case PREMIUM_RECONNECT_REQUIRED -> {
                player.connection.disconnect(Component.literal(
                        "Logout realizado. Reconecte para autenticar sua conta Premium novamente."
                ));
                yield 1;
            }
            case ALREADY_LOGGED_OUT -> {
                source.sendFailure(Component.literal("§eVocê já está desconectado da sua conta."));
                yield 0;
            }
            case SESSION_NOT_FOUND -> {
                source.sendFailure(Component.literal("§cSua sessão ainda não foi carregada."));
                yield 0;
            }
        };
    }
}
