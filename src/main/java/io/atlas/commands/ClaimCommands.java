package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.claim.ClaimModule;
import io.atlas.modules.claim.model.Claim;
import io.atlas.modules.claim.model.TrustLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ClaimCommands {
    private ClaimCommands() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        trust(dispatcher, "trust", TrustLevel.BUILD);
        trust(dispatcher, "containertrust", TrustLevel.CONTAINER);
        trust(dispatcher, "accesstrust", TrustLevel.ACCESS);
        dispatcher.register(Commands.literal("untrust").requires(CommandSourceStack::isPlayer)
                .then(Commands.argument("jogador", StringArgumentType.word()).executes(ctx ->
                        ClaimModule.getService().removeTrust(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "jogador")) ? 1 : 0)));
        dispatcher.register(Commands.literal("abandonclaim").requires(CommandSourceStack::isPlayer)
                .executes(ctx -> ClaimModule.getService().abandon(ctx.getSource().getPlayerOrException()) ? 1 : 0));
        dispatcher.register(Commands.literal("claimslist").requires(CommandSourceStack::isPlayer)
                .executes(ctx -> list(ctx.getSource())));
    }
    private static void trust(CommandDispatcher<CommandSourceStack> dispatcher, String command, TrustLevel level) {
        dispatcher.register(Commands.literal(command).requires(CommandSourceStack::isPlayer)
                .then(Commands.argument("jogador", StringArgumentType.word()).executes(ctx ->
                        ClaimModule.getService().setTrust(ctx.getSource().getPlayerOrException(),
                                StringArgumentType.getString(ctx, "jogador"), level) ? 1 : 0)));
    }
    private static int list(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var player = source.getPlayerOrException();
        List<Claim> claims = ClaimModule.getService().list(player.getUUID());
        if (claims.isEmpty()) {
            source.sendFailure(Component.literal("§eVocê ainda não possui claims."));
            return 0;
        }
        int used = claims.stream().mapToInt(Claim::area).sum();
        source.sendSuccess(() -> Component.literal("§6Claims §7(" + claims.size() + ") §8— §f" + used + "/"
                + ClaimModule.getService().areaLimit(player.getUUID()) + " blocos"), false);
        for (int i = 0; i < claims.size(); i++) {
            Claim claim = claims.get(i);
            int number = i + 1;
            source.sendSuccess(() -> Component.literal("§7#" + number + " §f" + claim.minX() + "," + claim.minZ()
                    + " até " + claim.maxX() + "," + claim.maxZ() + " §8(" + claim.area() + ")"), false);
        }
        return claims.size();
    }
}
