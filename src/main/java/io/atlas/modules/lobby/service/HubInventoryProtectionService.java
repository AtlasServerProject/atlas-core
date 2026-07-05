package io.atlas.modules.lobby.service;

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

    public boolean canKeepItem(ServerPlayer player, ItemStack stack) {
        if (!LobbyWorlds.isAuth(player.level())) {
            return true;
        }
        return isAllowedHubItem(stack);
    }

    public void enforce(ServerPlayer player) {
        if (!LobbyWorlds.isAuth(player.level())) {
            return;
        }

        Inventory inventory = player.getInventory();
        inventory.clearContent();
        inventory.setItem(COMPASS_SLOT, createCompass());
        inventory.setChanged();
    }

    private boolean isAllowedHubItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return stack.is(Items.COMPASS);
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
