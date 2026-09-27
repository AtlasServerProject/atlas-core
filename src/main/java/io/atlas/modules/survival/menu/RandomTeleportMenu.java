package io.atlas.modules.survival.menu;

import io.atlas.modules.survival.service.RandomTeleportService;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class RandomTeleportMenu extends ChestMenu {

    private static final int OVERWORLD_SLOT = 2;
    private static final int NETHER_SLOT = 4;
    private static final int END_SLOT = 6;

    private final RandomTeleportService randomTeleportService;

    public RandomTeleportMenu(
            int containerId,
            Inventory inventory,
            SimpleContainer container,
            RandomTeleportService randomTeleportService
    ) {
        super(MenuType.GENERIC_9x1, containerId, inventory, container, 1);
        this.randomTeleportService = randomTeleportService;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }

        RandomTeleportService.RtpTarget target = switch (slotId) {
            case OVERWORLD_SLOT -> RandomTeleportService.RtpTarget.OVERWORLD;
            case NETHER_SLOT -> RandomTeleportService.RtpTarget.NETHER;
            case END_SLOT -> RandomTeleportService.RtpTarget.END;
            default -> null;
        };
        if (target == null) {
            return;
        }

        serverPlayer.closeContainer();
        randomTeleportService.request(serverPlayer, target);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
