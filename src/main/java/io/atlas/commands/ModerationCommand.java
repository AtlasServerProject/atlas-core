package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.atlas.modules.moderation.ModerationModule;
import io.atlas.modules.moderation.model.ModerationDuration;
import io.atlas.modules.moderation.service.ModerationService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

public final class ModerationCommand {

    private static final ModerationService SERVICE = ModerationModule.getService();

    private ModerationCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("warn")
                .requires(SERVICE::canModerate)
                .then(Commands.argument("jogador", StringArgumentType.word())
                        .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                .executes(context -> send(context.getSource(), SERVICE.warn(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "jogador"),
                                        StringArgumentType.getString(context, "motivo")
                                ))))));

        dispatcher.register(Commands.literal("kick")
                .requires(SERVICE::canModerate)
                .then(Commands.argument("jogador", EntityArgument.player())
                        .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                .executes(context -> send(context.getSource(), SERVICE.kick(
                                        context.getSource(),
                                        EntityArgument.getPlayer(context, "jogador"),
                                        StringArgumentType.getString(context, "motivo")
                                ))))));

        dispatcher.register(Commands.literal("mute")
                .requires(SERVICE::canModerate)
                .then(Commands.argument("jogador", StringArgumentType.word())
                        .then(Commands.argument("tempo", StringArgumentType.word())
                                .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                        .executes(context -> {
                                            var duration = ModerationDuration.parse(
                                                    StringArgumentType.getString(context, "tempo")
                                            );
                                            if (duration.isEmpty()) {
                                                context.getSource().sendFailure(Component.literal(
                                                        "§cTempo inválido. Use exemplos como §f10m§c, §f2h§c, §f7d§c ou §fperma§c."
                                                ));
                                                return 0;
                                            }
                                            return send(context.getSource(), SERVICE.mute(
                                                    context.getSource(),
                                                    StringArgumentType.getString(context, "jogador"),
                                                    duration.get(),
                                                    StringArgumentType.getString(context, "motivo")
                                            ));
                                        })))));

        dispatcher.register(Commands.literal("unmute")
                .requires(SERVICE::canModerate)
                .then(Commands.argument("jogador", StringArgumentType.word())
                        .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                .executes(context -> send(context.getSource(), SERVICE.unmute(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "jogador"),
                                        StringArgumentType.getString(context, "motivo")
                                ))))));

        dispatcher.register(Commands.literal("ban")
                .requires(SERVICE::canModerate)
                .then(banArguments()));

        dispatcher.register(Commands.literal("atlasban")
                .requires(SERVICE::canModerate)
                .then(banArguments()));

        dispatcher.register(Commands.literal("unban")
                .requires(SERVICE::canModerate)
                .then(unbanArguments()));

        dispatcher.register(Commands.literal("atlasunban")
                .requires(SERVICE::canModerate)
                .then(unbanArguments()));

        dispatcher.register(Commands.literal("banip")
                .requires(SERVICE::canModerate)
                .then(Commands.argument("ip_ou_jogador", StringArgumentType.word())
                        .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                .executes(context -> send(context.getSource(), SERVICE.banIp(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "ip_ou_jogador"),
                                        StringArgumentType.getString(context, "motivo")
                                ))))));

        dispatcher.register(Commands.literal("unbanip")
                .requires(SERVICE::canModerate)
                .then(Commands.argument("ip", StringArgumentType.word())
                        .then(Commands.argument("motivo", StringArgumentType.greedyString())
                                .executes(context -> send(context.getSource(), SERVICE.unbanIp(
                                        context.getSource(),
                                        StringArgumentType.getString(context, "ip"),
                                        StringArgumentType.getString(context, "motivo")
                                ))))));

        dispatcher.register(Commands.literal("punishments")
                .requires(SERVICE::canModerate)
                .then(Commands.argument("jogador", StringArgumentType.word())
                        .executes(context -> history(
                                context.getSource(),
                                StringArgumentType.getString(context, "jogador")
                        ))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> banArguments() {
        return Commands.argument("jogador", StringArgumentType.word())
                .then(Commands.argument("motivo", StringArgumentType.greedyString())
                        .executes(context -> send(context.getSource(), SERVICE.ban(
                                context.getSource(),
                                StringArgumentType.getString(context, "jogador"),
                                StringArgumentType.getString(context, "motivo")
                        ))));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> unbanArguments() {
        return Commands.argument("jogador", StringArgumentType.word())
                .then(Commands.argument("motivo", StringArgumentType.greedyString())
                        .executes(context -> send(context.getSource(), SERVICE.unban(
                                context.getSource(),
                                StringArgumentType.getString(context, "jogador"),
                                StringArgumentType.getString(context, "motivo")
                        ))));
    }

    /*
     * O Minecraft já registra /ban vanilla. Mantemos /ban por compatibilidade,
     * mas /atlasban e /atlasunban são os caminhos explícitos do Atlas.
     */
    private static int history(CommandSourceStack source, String username) {
        var punishments = SERVICE.history(username);
        if (punishments.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§eNenhuma punição encontrada para §f" + username + "§e."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("§6Punições recentes de §f" + username + "§6:"), false);
        for (var punishment : punishments) {
            source.sendSuccess(() -> Component.literal(SERVICE.formatPunishment(punishment)), false);
        }
        return punishments.size();
    }

    private static int send(CommandSourceStack source, ModerationService.Result result) {
        if (result.success()) {
            source.sendSuccess(() -> Component.literal(result.message()), false);
            return 1;
        }
        source.sendFailure(Component.literal(result.message()));
        return 0;
    }
}
