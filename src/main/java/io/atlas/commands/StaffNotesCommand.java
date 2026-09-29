package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.moderation.service.StaffNotesService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class StaffNotesCommand {
    private static final StaffNotesService SERVICE = new StaffNotesService();
    private StaffNotesCommand() {}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("staffnotes").requires(SERVICE::canUse)
                .then(Commands.literal("add").then(Commands.argument("jogador", StringArgumentType.word())
                        .then(Commands.argument("texto", StringArgumentType.greedyString()).executes(c -> SERVICE.add(
                                c.getSource(), StringArgumentType.getString(c, "jogador"), StringArgumentType.getString(c, "texto"))))))
                .then(Commands.literal("list").then(listArguments(false)))
                .then(Commands.literal("archive").then(Commands.argument("jogador", StringArgumentType.word())
                        .then(Commands.argument("id", LongArgumentType.longArg(1))
                                .then(Commands.argument("motivo", StringArgumentType.greedyString()).executes(c -> SERVICE.archive(
                                        c.getSource(), StringArgumentType.getString(c, "jogador"), LongArgumentType.getLong(c, "id"),
                                        StringArgumentType.getString(c, "motivo"))))))));
        dispatcher.register(Commands.literal("history").requires(SERVICE::canUse).then(listArguments(true)));
    }
    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> listArguments(boolean history) {
        return Commands.argument("jogador", StringArgumentType.word())
                .executes(c -> SERVICE.list(c.getSource(), StringArgumentType.getString(c, "jogador"), 1, history))
                .then(Commands.argument("pagina", IntegerArgumentType.integer(1, 1000000)).executes(c -> SERVICE.list(
                        c.getSource(), StringArgumentType.getString(c, "jogador"), IntegerArgumentType.getInteger(c, "pagina"), history)));
    }
}
