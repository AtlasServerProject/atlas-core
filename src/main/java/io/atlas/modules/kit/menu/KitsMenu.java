package io.atlas.modules.kit.menu;

import io.atlas.modules.kit.KitModule;
import io.atlas.modules.kit.service.DailyKitService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;


public final class KitsMenu extends ChestMenu {


    public KitsMenu(int containerId, Inventory inventory, SimpleContainer container) {
        super(MenuType.GENERIC_9x4, containerId, inventory, container, 4);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        DailyKitService service = KitModule.getDailyKitService();
        var kit = service.findBySlot(slotId);
        if (kit.isEmpty()) {
            return;
        }

        serverPlayer.closeContainer();
        DailyKitService.ClaimResult result = service.claim(serverPlayer, kit.get());
        if (result.locked()) {
            serverPlayer.displayClientMessage(
                    Component.literal("§cVocê não possui o cargo necessário para resgatar esse kit."),
                    false
            );
            return;
        }
        if (!result.claimed()) {
            serverPlayer.displayClientMessage(
                    Component.literal("§eVocê já resgatou o " + result.kit().displayName()
                            + ". Tente novamente em §f" + service.format(result.remaining()) + "§e."),
                    false
            );
            return;
        }

        serverPlayer.displayClientMessage(
                Component.literal(result.kit().color() + result.kit().displayName()
                        + " resgatado com sucesso!"),
                false
        );
        service.openMintChoiceMenu(serverPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

}
