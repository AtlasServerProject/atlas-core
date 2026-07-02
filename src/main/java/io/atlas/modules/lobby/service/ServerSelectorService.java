package io.atlas.modules.lobby.service;

import io.atlas.modules.auth.service.AuthService;
import io.atlas.modules.lobby.menu.ServerSelectorMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.phys.Vec3;

public class ServerSelectorService {

    private static final String SELECTOR_MARKER = "atlas_server_selector";
    private static final int SELECTOR_SLOT = 4;
    private static final int MAIN_INVENTORY_SIZE = 36;
    private static final int SYNC_INTERVAL_TICKS = 20;
    private static final ResourceKey<Level> EMERALD_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("atlas", "emerald")
    );
    private static final double EMERALD_X = 975.5;
    private static final double EMERALD_Y = 179.0;
    private static final double EMERALD_Z = 1573.5;
    private static final float EMERALD_YAW = -90.0F;

    private final AuthService authService;
    private int ticks;

    public ServerSelectorService(AuthService authService) {
        this.authService = authService;
    }

    public void tick(MinecraftServer server) {
        ticks++;
        if (ticks < SYNC_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (isAuthLobby(player) && authService.isAuthenticated(player.getUUID())) {
                ensureSelector(player);
            } else {
                removeSelectors(player.getInventory());
            }
        }
    }

    public boolean isSelector(ItemStack stack) {
        if (!stack.is(Items.COMPASS)) {
            return false;
        }

        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBoolean(SELECTOR_MARKER);
    }

    public boolean shouldBlockInventoryClick(
            ServerPlayer player,
            ItemStack carriedItem,
            Iterable<ItemStack> changedItems,
            int clickedSlot
    ) {
        if (isSelector(carriedItem)) {
            return true;
        }
        for (ItemStack changedItem : changedItems) {
            if (isSelector(changedItem)) {
                return true;
            }
        }
        if (clickedSlot >= 0 && clickedSlot < player.containerMenu.slots.size()) {
            return isSelector(player.containerMenu.getSlot(clickedSlot).getItem());
        }
        return false;
    }

    public boolean shouldBlockCreativeSlot(ServerPlayer player, int slot, ItemStack item) {
        if (isSelector(item)) {
            return true;
        }
        return slot >= 0
                && slot < player.inventoryMenu.slots.size()
                && isSelector(player.inventoryMenu.getSlot(slot).getItem());
    }

    public void openSelector(ServerPlayer player) {
        if (!authService.isAuthenticated(player.getUUID()) || !isAuthLobby(player)) {
            player.displayClientMessage(
                    Component.literal("§cO seletor só pode ser usado no Auth Lobby após o login."),
                    true
            );
            return;
        }

        SimpleContainer container = new SimpleContainer(9);
        container.setItem(4, createEmeraldOption());
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new ServerSelectorMenu(
                        containerId,
                        inventory,
                        container,
                        this
                ),
                Component.literal("§2Escolha um servidor")
        ));
    }

    public void selectEmerald(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || !authService.isAuthenticated(serverPlayer.getUUID())) {
            return;
        }

        ServerLevel emerald = serverPlayer.getServer().getLevel(EMERALD_LEVEL);
        if (emerald == null) {
            serverPlayer.displayClientMessage(
                    Component.literal("§cO Lobby Emerald está temporariamente indisponível."),
                    false
            );
            return;
        }

        serverPlayer.closeContainer();
        removeSelectors(serverPlayer.getInventory());
        serverPlayer.setDeltaMovement(Vec3.ZERO);
        serverPlayer.teleportTo(
                emerald,
                EMERALD_X,
                EMERALD_Y,
                EMERALD_Z,
                EMERALD_YAW,
                0.0F
        );
        serverPlayer.displayClientMessage(
                Component.literal("§aBem-vindo ao Lobby Emerald!"),
                false
        );
    }

    private void ensureSelector(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int freedSlot = removeSelectorsOutsideReservedSlot(inventory);
        if (isSelector(inventory.getItem(SELECTOR_SLOT))) {
            return;
        }

        ItemStack displaced = inventory.getItem(SELECTOR_SLOT);
        if (!displaced.isEmpty()) {
            int destination = freedSlot >= 0 ? freedSlot : findEmptySlot(inventory);
            if (destination < 0) {
                player.displayClientMessage(
                        Component.literal("§cLibere um espaço no inventário para receber o seletor."),
                        true
                );
                return;
            }
            inventory.setItem(destination, displaced);
        }

        inventory.setItem(SELECTOR_SLOT, createSelector());
        inventory.setChanged();
    }

    private int removeSelectorsOutsideReservedSlot(Inventory inventory) {
        int freedSlot = -1;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (slot != SELECTOR_SLOT && isSelector(inventory.getItem(slot))) {
                inventory.setItem(slot, ItemStack.EMPTY);
                if (slot < MAIN_INVENTORY_SIZE && freedSlot < 0) {
                    freedSlot = slot;
                }
            }
        }
        return freedSlot;
    }

    private void removeSelectors(Inventory inventory) {
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

    private int findEmptySlot(Inventory inventory) {
        for (int slot = 0; slot < MAIN_INVENTORY_SIZE; slot++) {
            if (slot != SELECTOR_SLOT && inventory.getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    private ItemStack createSelector() {
        ItemStack compass = new ItemStack(Items.COMPASS);
        CompoundTag marker = new CompoundTag();
        marker.putBoolean(SELECTOR_MARKER, true);
        compass.set(DataComponents.CUSTOM_DATA, CustomData.of(marker));
        compass.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("§aSeletor de Servidores")
        );
        compass.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return compass;
    }

    private ItemStack createEmeraldOption() {
        ItemStack emerald = new ItemStack(Items.EMERALD);
        emerald.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("§aLobby Emerald")
        );
        emerald.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return emerald;
    }

    private boolean isAuthLobby(ServerPlayer player) {
        return player.level().dimension() == Level.OVERWORLD;
    }
}
