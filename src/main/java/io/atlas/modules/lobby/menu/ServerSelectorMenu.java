package io.atlas.modules.lobby.menu;

import io.atlas.modules.lobby.service.ServerSelectorService;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class ServerSelectorMenu extends ChestMenu {

    private static final int EMERALD_SLOT = 4;

    private final ServerSelectorService selectorService;

    public ServerSelectorMenu(
            int containerId,
            Inventory inventory,
            SimpleContainer container,
            ServerSelectorService selectorService
    ) {
        super(MenuType.GENERIC_9x1, containerId, inventory, container, 1);
        this.selectorService = selectorService;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == EMERALD_SLOT) {
            selectorService.selectEmerald(player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
