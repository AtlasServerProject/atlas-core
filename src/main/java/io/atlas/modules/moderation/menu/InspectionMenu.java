package io.atlas.modules.moderation.menu;

import io.atlas.modules.moderation.service.StaffToolsService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class InspectionMenu extends ChestMenu {
    private final ServerPlayer viewer;
    private final ServerPlayer target;
    private final StaffToolsService service;
    private final SimpleContainer display;
    private final boolean ender;

    public static void open(ServerPlayer viewer, ServerPlayer target, boolean ender, StaffToolsService service) {
        viewer.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                new InspectionMenu(id, inventory, viewer, target, ender, service),
                Component.literal((ender ? "Ender Chest: " : "Inventário: ") + target.getName().getString())));
    }

    private InspectionMenu(int id, Inventory inventory, ServerPlayer viewer, ServerPlayer target,
                           boolean ender, StaffToolsService service) {
        this(id, inventory, viewer, target, ender, service, new SimpleContainer(ender ? 36 : 54));
    }

    private InspectionMenu(int id, Inventory inventory, ServerPlayer viewer, ServerPlayer target,
                           boolean ender, StaffToolsService service, SimpleContainer display) {
        super(ender ? MenuType.GENERIC_9x4 : MenuType.GENERIC_9x6, id, inventory, display, ender ? 4 : 6);
        this.viewer = viewer;
        this.target = target;
        this.service = service;
        this.display = display;
        this.ender = ender;
        refresh();
    }

    private void refresh() {
        for (int i = 0; i < display.getContainerSize(); i++) display.setItem(i, ItemStack.EMPTY);
        if (ender) {
            for (int i = 0; i < 27; i++) display.setItem(i, target.getEnderChestInventory().getItem(i).copy());
        } else {
            for (int i = 0; i < 27; i++) display.setItem(i, target.getInventory().getItem(i + 9).copy());
            for (int i = 0; i < 9; i++) display.setItem(27 + i, target.getInventory().getItem(i).copy());
            for (int i = 36; i <= 40; i++) display.setItem(i, target.getInventory().getItem(i).copy());
        }
        ItemStack info = new ItemStack(Items.PAPER);
        info.set(DataComponents.CUSTOM_NAME, Component.literal(ender ? "§eSomente leitura" : "§eSomente leitura — última linha de itens: botas → capacete, mão secundária"));
        display.setItem(ender ? 31 : 49, info);
    }

    @Override
    public boolean stillValid(Player player) {
        return player == viewer && !target.hasDisconnected() && !viewer.hasDisconnected()
                && service.canInspect(viewer, target);
    }

    @Override
    public void broadcastChanges() {
        if (service != null) {
            if (!stillValid(viewer)) { viewer.closeContainer(); return; }
            refresh();
        }
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        // Never delegate any click type: copies cannot be picked up, swapped, dropped or dragged.
        if (player == viewer) sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
