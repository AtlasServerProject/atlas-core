package io.atlas.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.kit.KitModule;
import io.atlas.modules.kit.menu.KitsMenu;
import io.atlas.modules.kit.service.DailyKitService;
import io.atlas.modules.player.listener.PlayerJoinListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;

public final class KitCommand {

    private KitCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("kit")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> openKits(context.getSource()))
                        .then(Commands.literal("diario")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.DAILY)))
                        .then(Commands.literal("daily")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.DAILY)))
                        .then(Commands.literal("semanal")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.WEEKLY)))
                        .then(Commands.literal("weekly")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.WEEKLY)))
                        .then(Commands.literal("mensal")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.MONTHLY)))
                        .then(Commands.literal("monthly")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.MONTHLY)))
                        .then(Commands.literal("vip")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP)))
                        .then(Commands.literal("vipdiario")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP)))
                        .then(Commands.literal("vipdaily")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP)))
                        .then(Commands.literal("vipsemanal")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_WEEKLY)))
                        .then(Commands.literal("vipweekly")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_WEEKLY)))
                        .then(Commands.literal("vipmensal")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_MONTHLY)))
                        .then(Commands.literal("vipmonthly")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_MONTHLY)))
                        .then(Commands.literal("vip+")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_PLUS)))
                        .then(Commands.literal("vipplus")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_PLUS)))
                        .then(Commands.literal("vipplusdiario")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_PLUS)))
                        .then(Commands.literal("vipplussemanal")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_PLUS_WEEKLY)))
                        .then(Commands.literal("vipplusmensal")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_PLUS_MONTHLY)))
                        .then(Commands.literal("vip++")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_PLUS_PLUS)))
                        .then(Commands.literal("vipplusplus")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_PLUS_PLUS)))
                        .then(Commands.literal("vipplusplusdiario")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_PLUS_PLUS)))
                        .then(Commands.literal("vipplusplussemanal")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_PLUS_PLUS_WEEKLY)))
                        .then(Commands.literal("vipplusplusmensal")
                                .executes(context -> claim(context.getSource(), DailyKitService.KitType.VIP_PLUS_PLUS_MONTHLY)))
        );
        dispatcher.register(
                Commands.literal("kits")
                        .requires(CommandSourceStack::isPlayer)
                        .executes(context -> openKits(context.getSource()))
        );
    }

    private static int openKits(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!canUseKits(source, player)) {
            return 0;
        }

        SimpleContainer container = new SimpleContainer(36);
        DailyKitService service = KitModule.getDailyKitService();
        for (DailyKitService.KitType kit : DailyKitService.KitType.values()) {
            container.setItem(kit.slot(), service.createIcon(kit));
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new KitsMenu(containerId, inventory, container),
                Component.literal("§2Kits disponíveis")
        ));
        return 1;
    }

    private static int claim(CommandSourceStack source, DailyKitService.KitType kit) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!canUseKits(source, player)) {
            return 0;
        }

        DailyKitService service = KitModule.getDailyKitService();
        DailyKitService.ClaimResult result = service.claim(player, kit);
        if (result.locked()) {
            source.sendFailure(Component.literal("§cVocê não possui o cargo necessário para resgatar esse kit."));
            return 0;
        }
        if (!result.claimed()) {
            source.sendFailure(Component.literal("§eVocê já resgatou o " + result.kit().displayName()
                    + ". Tente novamente em §f" + service.format(result.remaining()) + "§e."));
            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(result.kit().color() + result.kit().displayName()
                        + " resgatado com sucesso!"),
                false
        );
        service.openMintChoiceMenu(player);
        return 1;
    }

    private static boolean canUseKits(CommandSourceStack source, ServerPlayer player) {
        if (!AuthModule.getAuthService().isAuthenticated(player.getUUID())) {
            source.sendFailure(Component.literal("§cFaça login antes de resgatar kits."));
            return false;
        }

        if (PlayerJoinListener.getPlayerService().getProfile(player.getUUID()).isEmpty()) {
            source.sendFailure(Component.literal("§cSeu perfil ainda não foi carregado."));
            return false;
        }

        return true;
    }
}
