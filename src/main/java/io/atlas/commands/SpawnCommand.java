package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.auth.service.AuthLobbySpawnService;
import io.atlas.modules.auth.service.AuthService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public class SpawnCommand {

    private static final AuthService authService = AuthModule.getAuthService();
    private static final AuthLobbySpawnService spawnService = new AuthLobbySpawnService();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("spawn")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> execute(context.getSource()))
        );
    }

    private static int execute(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException();
        if (!authService.isAuthenticated(player.getUUID())) {
            source.sendFailure(Component.literal(
                    "§cVocê precisa se autenticar antes de usar /spawn."
            ));
            return 0;
        }

        player.setDeltaMovement(Vec3.ZERO);
        spawnService.teleportToSpawn(player, source.getServer());
        source.sendSuccess(
                () -> Component.literal("§aVocê voltou ao Hub do Atlas."),
                false
        );
        return 1;
    }
}
