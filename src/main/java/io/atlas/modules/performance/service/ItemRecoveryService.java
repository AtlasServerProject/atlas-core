package io.atlas.modules.performance.service;

import io.atlas.AtlasMod;
import io.atlas.modules.performance.menu.RecoveryContainer;
import io.atlas.modules.performance.repository.ItemRecoveryRepository;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ItemRecoveryService {
    private static final int SIZE = 54;
    private static final Duration RETENTION = Duration.ofMinutes(15);
    private final ItemRecoveryRepository repository = new ItemRecoveryRepository();
    private int expiryTicks;

    public void openTrash(ServerPlayer player) {
        RecoveryContainer container = new RecoveryContainer(SIZE, remaining -> {
            store(player.getUUID(), "TRASH", remaining, player.server);
            if (!remaining.isEmpty()) player.displayClientMessage(Component.literal(
                    "§e" + remaining.size() + " pilhas foram enviadas à lixeira. Use §f/dropados §eem até 15 minutos."), false);
        });
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                ChestMenu.sixRows(id, inventory, container), Component.literal("Lixeira — recuperável por 15 min")));
    }

    public boolean openRecovery(ServerPlayer player) {
        var entries = repository.findAvailable(player.getUUID(), SIZE);
        if (entries.isEmpty()) {
            player.displayClientMessage(Component.literal("§eVocê não possui itens recuperáveis."), false);
            return false;
        }
        List<Long> ids = entries.stream().map(ItemRecoveryRepository.RecoveryEntry::id).toList();
        RecoveryContainer container = new RecoveryContainer(SIZE, remaining -> {
            store(player.getUUID(), "RECOVERY_REMAINDER", remaining, player.server);
            AtlasMod.LOGGER.info("{} fechou /dropados: {} entradas abertas, {} pilhas mantidas.",
                    player.getGameProfile().getName(), entries.size(), remaining.size());
        });
        int slot = 0;
        for (var entry : entries) {
            ItemStack stack = decode(entry.itemSnbt(), player.server);
            if (!stack.isEmpty()) container.setItem(slot++, stack);
        }
        repository.markRecovered(ids);
        player.openMenu(new SimpleMenuProvider((id, inventory, ignored) ->
                ChestMenu.sixRows(id, inventory, container), Component.literal("Itens recuperáveis")));
        return true;
    }

    public void store(UUID uuid, String source, List<ItemStack> stacks, MinecraftServer server) {
        List<String> encoded = stacks.stream().filter(stack -> !stack.isEmpty())
                .map(stack -> stack.save(server.registryAccess()).toString()).toList();
        if (encoded.isEmpty()) return;
        repository.store(uuid, source, encoded, Instant.now().plus(RETENTION));
        AtlasMod.LOGGER.info("{} pilhas armazenadas para recuperação de {} (origem {}).", encoded.size(), uuid, source);
    }

    public void tick(MinecraftServer server) {
        if (++expiryTicks < 60 * 20) return;
        expiryTicks = 0;
        int expired = repository.deleteExpired();
        if (expired > 0) AtlasMod.LOGGER.info("{} entradas expiradas removidas da recuperação.", expired);
    }

    private ItemStack decode(String snbt, MinecraftServer server) {
        try {
            return ItemStack.parse(server.registryAccess(), TagParser.parseTag(snbt)).orElse(ItemStack.EMPTY);
        } catch (Exception exception) {
            AtlasMod.LOGGER.error("Falha ao desserializar item recuperável.", exception);
            return ItemStack.EMPTY;
        }
    }
}
