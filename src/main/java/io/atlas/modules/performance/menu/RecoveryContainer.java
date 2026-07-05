package io.atlas.modules.performance.menu;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public final class RecoveryContainer extends SimpleContainer {
    private final Consumer<List<ItemStack>> onClose;
    private boolean closed;

    public RecoveryContainer(int size, Consumer<List<ItemStack>> onClose) {
        super(size);
        this.onClose = onClose;
    }

    @Override
    public void stopOpen(Player player) {
        super.stopOpen(player);
        if (closed) return;
        closed = true;
        onClose.accept(getItems().stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).toList());
        clearContent();
    }
}
