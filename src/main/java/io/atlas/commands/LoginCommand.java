package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.auth.service.AuthService;
import io.atlas.modules.survival.SurvivalModule;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class LoginCommand {

    private static final AuthService authService = AuthModule.getAuthService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("login")
                        .requires(CommandSourceStack::isPlayer)
                        .then(Commands.argument("senha", StringArgumentType.word())
                                .executes(context -> execute(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "senha")
                                )))
        );
    }

    private static int execute(CommandSourceStack source, String password)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        AuthService.LoginResult result = authService.login(
                player.getUUID(),
                password,
                player.getIpAddress()
        );

        Component message = switch (result) {
            case SUCCESS -> Component.literal("§aLogin realizado com sucesso.");
            case NOT_REGISTERED -> Component.literal("§cConta não registrada. Use /register.");
            case ALREADY_AUTHENTICATED -> Component.literal("§eVocê já está autenticado.");
            case INVALID_PASSWORD -> Component.literal("§cSenha incorreta.");
            case COOLDOWN -> Component.literal(
                    "§cMuitas tentativas inválidas. Aguarde §e"
                            + formatCooldown(authService.getLoginCooldownSeconds(player.getUUID()))
                            + "§c para tentar novamente."
            );
            case PREMIUM_ACCOUNT -> Component.literal("§eContas Premium autenticam automaticamente.");
            case SESSION_NOT_FOUND -> Component.literal("§cSua sessão ainda não foi carregada.");
        };

        if (result == AuthService.LoginResult.SUCCESS
                || result == AuthService.LoginResult.ALREADY_AUTHENTICATED) {
            SurvivalModule.getPositionService().restore(player);
            source.sendSuccess(() -> message, false);
            return 1;
        }

        source.sendFailure(message);
        return 0;
    }

    private static String formatCooldown(long totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes > 0
                ? minutes + "min " + seconds + "s"
                : seconds + "s";
    }
}
