package io.atlas.modules.kit.menu;

import io.atlas.modules.kit.service.DailyKitService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class MintSelectionMenu extends ChestMenu {

    private final DailyKitService kitService;

    public MintSelectionMenu(
            int containerId,
            Inventory inventory,
            SimpleContainer container,
            DailyKitService kitService
    ) {
        super(MenuType.GENERIC_9x3, containerId, inventory, container, 3);
        this.kitService = kitService;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (kitService.chooseMint(serverPlayer, slotId)) {
            serverPlayer.closeContainer();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
