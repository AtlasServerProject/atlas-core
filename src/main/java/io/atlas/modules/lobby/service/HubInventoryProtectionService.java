package io.atlas.modules.lobby.service;

import io.atlas.modules.auth.service.AuthService;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HubInventoryProtectionService {

    private static final int COMPASS_SLOT = 4;
    private static final String SELECTOR_MARKER = "atlas_server_selector";
    private final AuthService authService;
    private final RankService rankService;
    private final Map<UUID, List<ItemStack>> savedInventories = new ConcurrentHashMap<>();

    public HubInventoryProtectionService(AuthService authService, RankService rankService) {
        this.authService = authService;
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

        saveOriginalInventory(player);
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isAllowedHubItem(player, stack)) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }

        if (!authService.isAuthenticated(player.getUUID())) {
            inventory.setChanged();
            return;
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

    public void restore(ServerPlayer player) {
        List<ItemStack> savedInventory = savedInventories.remove(player.getUUID());
        if (savedInventory == null) {
            removeSelector(player.getInventory());
            return;
        }

        Inventory inventory = player.getInventory();
        int size = Math.min(inventory.getContainerSize(), savedInventory.size());
        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, savedInventory.get(slot).copy());
        }
        for (int slot = size; slot < inventory.getContainerSize(); slot++) {
            inventory.setItem(slot, ItemStack.EMPTY);
        }
        inventory.setChanged();
    }

    private void saveOriginalInventory(ServerPlayer player) {
        savedInventories.computeIfAbsent(player.getUUID(), ignored -> {
            Inventory inventory = player.getInventory();
            List<ItemStack> snapshot = new ArrayList<>(inventory.getContainerSize());
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                snapshot.add(isSelector(stack) ? ItemStack.EMPTY : stack.copy());
            }
            return snapshot;
        });
    }

    private boolean isAllowedHubItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return (authService.isAuthenticated(player.getUUID()) && isSelector(stack))
                || (rankService.canManageRanks(player.getUUID()) && stack.is(Items.WOODEN_AXE));
    }

    private void removeSelector(Inventory inventory) {
        boolean changed = false;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isSelector(inventory.getItem(slot))) {
                inventory.setItem(slot, ItemStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            inventory.setChanged();
        }
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
