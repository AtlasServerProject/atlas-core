package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import io.atlas.modules.market.MarketModule;

public final class MarketCommand {
    private static int duration(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        try {return io.atlas.modules.market.model.MarketPolicy.parseDuration(StringArgumentType.getString(context,"tempo"));}
        catch(IllegalArgumentException error){throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(net.minecraft.network.chat.Component.literal(error.getMessage())).create();}
    }
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("gts")
                .requires(source -> source.isPlayer() && io.atlas.modules.auth.AuthModule.getAuthService()
                        .isAuthenticated(source.getPlayer().getUUID()))
                .executes(context -> MarketModule.service().open(context.getSource().getPlayerOrException(), "ALL", false, 0, ""));
        root.then(Commands.literal("meus").executes(context -> MarketModule.service()
                .open(context.getSource().getPlayerOrException(), "ALL", true, 0, "")));
        root.then(Commands.literal("ganhos").executes(context -> {
            MarketModule.service().withdraw(context.getSource().getPlayerOrException(), "ALL");
            return 1;
        }));
        root.then(Commands.literal("buscar").then(Commands.argument("nome", StringArgumentType.greedyString())
                .executes(context -> MarketModule.service().open(context.getSource().getPlayerOrException(),
                        "ALL", false, 0, StringArgumentType.getString(context, "nome")))));
        var pokemonPrice = Commands.argument("preco", LongArgumentType.longArg(1, 1000000000L))
                .executes(context -> MarketModule.service().draft(context.getSource().getPlayerOrException(),
                        "POKEMON", IntegerArgumentType.getInteger(context, "slot"), LongArgumentType.getLong(context, "preco")));
        pokemonPrice.then(Commands.argument("tempo", StringArgumentType.word())
                .executes(context -> MarketModule.service().draft(context.getSource().getPlayerOrException(),
                        "POKEMON", IntegerArgumentType.getInteger(context, "slot"), LongArgumentType.getLong(context, "preco"), duration(context))));
        var itemPrice = Commands.argument("preco", LongArgumentType.longArg(1, 1000000000L))
                .executes(context -> MarketModule.service().draftItem(context.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(context, "quantia"), LongArgumentType.getLong(context, "preco"), 86400));
        itemPrice.then(Commands.argument("tempo", StringArgumentType.word())
                .executes(context -> MarketModule.service().draftItem(context.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(context, "quantia"), LongArgumentType.getLong(context, "preco"), duration(context))));
        root.then(Commands.literal("sell")
                .then(Commands.literal("pokemon").then(Commands.argument("slot", IntegerArgumentType.integer(1, 6)).then(pokemonPrice)))
                .then(Commands.literal("item").then(Commands.argument("quantia", IntegerArgumentType.integer(1)).then(itemPrice))));
        dispatcher.register(root);
    }
}
