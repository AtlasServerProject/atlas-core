package io.atlas.modules.lobby.service;

import io.atlas.modules.rank.service.RankService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public class HubInventoryProtectionService {

    private static final int COMPASS_SLOT = 4;
    private static final String SELECTOR_MARKER = "atlas_server_selector";
    private final RankService rankService;

    public HubInventoryProtectionService(RankService rankService) {
        this.rankService = rankService;
    }

    public boolean canKeepItem(ServerPlayer player, ItemStack stack) {
        if (!LobbyWorlds.isAuth(player.level())) {
            return true;
        }
        return isAllowedHubItem(player, stack);
    }

    public void enforce(ServerPlayer player) {
        if (!LobbyWorlds.isAuth(player.level())) {
            return;
        }

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isAllowedHubItem(player, stack)) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }

        ItemStack reservedSlot = inventory.getItem(COMPASS_SLOT);
        if (!reservedSlot.isEmpty() && !isSelector(reservedSlot)) {
            int destination = findEmptySlot(inventory);
            if (destination >= 0) {
                inventory.setItem(destination, reservedSlot);
            }
        }
        inventory.setItem(COMPASS_SLOT, createCompass());
        inventory.setChanged();
    }

    private boolean isAllowedHubItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return isSelector(stack)
                || (rankService.canManageRanks(player.getUUID()) && stack.is(Items.WOODEN_AXE));
    }

    private boolean isSelector(ItemStack stack) {
        if (!stack.is(Items.COMPASS)) {
            return false;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(SELECTOR_MARKER);
    }

    private int findEmptySlot(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (slot != COMPASS_SLOT && inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private ItemStack createCompass() {
        ItemStack compass = new ItemStack(Items.COMPASS);
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(SELECTOR_MARKER, true);
        compass.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        compass.set(DataComponents.CUSTOM_NAME, Component.literal("§aSeletor de Servidores"));
        return compass;
    }
}
