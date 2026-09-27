package io.atlas.modules.home.menu;

import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.home.model.Home;
import io.atlas.modules.home.service.HomeService;
import io.atlas.modules.lobby.service.LobbyWorlds;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

import java.util.List;
import java.util.UUID;

/** Server-side menu: displayed items never enter a player's inventory. */
public final class HomesMenu extends ChestMenu {
    private static final int PAGE_SIZE = 28;
    private final SimpleContainer display;
    private final HomeService service;
    private final UUID owner;
    private List<Home> homes = List.of();
    private int page;
    private boolean deleting;
    private Action confirmation;
    private Home selected;
    private String newName;
    private long shownSeconds = -1;
    private final ServerPlayer viewer;

    private enum Action { CREATE, UPDATE, DELETE }

    public static void open(ServerPlayer player, HomeService service) {
        if (!allowed(player)) return;
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> new HomesMenu(id, inventory, player, service),
                Component.literal("Homes do Atlas")));
    }

    private HomesMenu(int id, Inventory inventory, ServerPlayer viewer, HomeService service) {
        this(id, inventory, viewer, service, new SimpleContainer(54));
    }

    private HomesMenu(int id, Inventory inventory, ServerPlayer viewer, HomeService service, SimpleContainer display) {
        super(MenuType.GENERIC_9x6, id, inventory, display, 6);
        this.display = display;
        this.service = service;
        this.viewer = viewer;
        this.owner = viewer.getUUID();
        refresh();
    }

    private static boolean allowed(ServerPlayer player) {
        return AuthModule.getAuthService().isAuthenticated(player.getUUID()) && !LobbyWorlds.isAuth(player.level());
    }

    @Override
    public boolean stillValid(Player player) {
        return player instanceof ServerPlayer serverPlayer && owner.equals(player.getUUID()) && allowed(serverPlayer);
    }

    private void refresh() {
        homes = service.listHomes(owner);
        page = Math.min(page, pages() - 1);
        render();
    }

    private int pages() {
        return Math.max(1, (Math.max(homes.size(), service.homeLimit(owner)) + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private int contentIndex(int slot) {
        int row = slot / 9;
        int column = slot % 9;
        return row >= 1 && row <= 4 && column >= 1 && column <= 7 ? (row - 1) * 7 + column - 1 : -1;
    }

    private void render() {
        for (int i = 0; i < 54; i++) display.setItem(i, icon(Items.GRAY_STAINED_GLASS_PANE, " "));
        if (confirmation != null) {
            String name = confirmation == Action.CREATE ? newName : selected.name();
            String action = switch (confirmation) {
                case CREATE -> "Criar ";
                case UPDATE -> "Atualizar ";
                case DELETE -> "Excluir ";
            };
            display.setItem(22, icon(Items.PAPER, "§e" + action + name,
                    confirmation == Action.DELETE ? "§7Esta exclusão não pode ser desfeita." : "§7Salvará sua posição ao confirmar."));
            display.setItem(30, icon(Items.LIME_DYE, "§aConfirmar"));
            display.setItem(32, icon(Items.RED_DYE, "§cCancelar"));
            return;
        }
        int limit = service.homeLimit(owner);
        for (int slot = 10; slot <= 43; slot++) {
            int offset = contentIndex(slot);
            if (offset < 0) continue;
            int index = page * PAGE_SIZE + offset;
            if (index < homes.size()) {
                Home home = homes.get(index);
                display.setItem(slot, icon(home.primary() ? Items.LIME_BED : Items.BLUE_BED,
                        (home.primary() ? "§a★ " : "§b") + home.name(),
                        "§7X: " + (int) home.x() + " Y: " + (int) home.y() + " Z: " + (int) home.z(),
                        deleting ? "§cClique para excluir (com confirmação)" : "§aEsquerdo: teleportar",
                        "§eShift + esquerdo: tornar principal", "§eDireito: atualizar posição (confirmar)"));
            } else if (index < limit) {
                display.setItem(slot, icon(Items.MAP, "§aHome disponível", "§7Clique para criar no Survival Emerald."));
            } else {
                display.setItem(slot, icon(Items.RED_STAINED_GLASS_PANE, "§cSlot bloqueado",
                        "§7Limite do seu cargo: " + limit, "§7Player 2 • VIP 5 • VIP✦ 8 • VIP✦✦ 12",
                        "§7SUP/MOD 25 • ADM/Dono 100"));
            }
        }
        display.setItem(4, icon(Items.PAPER, "§6Suas homes: " + homes.size() + "/" + limit,
                "§7Página " + (page + 1) + "/" + pages(), "§7Nome personalizado: /sethome <nome>"));
        if (page > 0) display.setItem(45, icon(Items.ARROW, "§ePágina anterior"));
        if (page + 1 < pages()) display.setItem(53, icon(Items.ARROW, "§ePróxima página"));
        if (homes.size() < limit && LobbyWorlds.isSurvivalEmerald(viewer.level()))
            display.setItem(47, icon(Items.COMPASS, "§aCriar home", "§7Confirme para salvar sua posição atual."));
        display.setItem(49, icon(Items.BARRIER, "§cFechar"));
        display.setItem(51, icon(Items.RED_DYE, deleting ? "§cExclusão ativada" : "§eModo excluir", "§7Clique para alternar."));
        updateClock();
    }

    private void updateClock() {
        shownSeconds = Math.max(0, (service.remainingCooldownMillis(owner) + 999) / 1000);
        display.setItem(48, icon(Items.CLOCK, shownSeconds == 0 ? "§aTeleporte disponível" : "§eAguarde " + shownSeconds + "s",
                "§7Aquecimento: 3 segundos. Não se mova."));
    }

    @Override
    public void broadcastChanges() {
        if (service != null && confirmation == null) {
            long seconds = Math.max(0, (service.remainingCooldownMillis(owner) + 999) / 1000);
            if (seconds != shownSeconds) updateClock();
        }
        super.broadcastChanges();
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !stillValid(player) || slot < 0 || slot >= 54) return;
        // Reject hotbar swaps, drag, drop, double-click and clone packets.
        if (type != ClickType.PICKUP && type != ClickType.QUICK_MOVE) return;
        if (button != 0 && button != 1) return;
        if (confirmation != null) {
            if (type != ClickType.PICKUP || button != 0) return;
            if (slot == 32) { confirmation = null; selected = null; refresh(); }
            else if (slot == 30) confirm(serverPlayer);
            return;
        }
        int offset = contentIndex(slot);
        if (offset >= 0) {
            int index = page * PAGE_SIZE + offset;
            if (index >= homes.size()) {
                if (type == ClickType.PICKUP && button == 0 && index < service.homeLimit(owner)) beginCreate(serverPlayer);
                return;
            }
            Home home = homes.get(index);
            if (!service.exists(owner, home)) { refresh(); return; }
            if (type == ClickType.QUICK_MOVE && button == 0) {
                message(serverPlayer, service.setPrimary(owner, home.name()) ? "§aHome principal definida." : "§cHome não encontrada.");
                refresh();
            } else if (type == ClickType.PICKUP && (deleting || button == 1)) {
                if (!deleting && !LobbyWorlds.isSurvivalEmerald(serverPlayer.level())) {
                    message(serverPlayer, "§cAtualize a posição apenas no Survival Emerald."); return;
                }
                selected = home;
                confirmation = deleting ? Action.DELETE : Action.UPDATE;
                render();
            } else if (type == ClickType.PICKUP && button == 0) {
                var result = service.requestTeleport(serverPlayer, home.name());
                switch (result) {
                    case STARTED -> serverPlayer.closeContainer();
                    case NOT_FOUND -> { message(serverPlayer, "§cHome não encontrada."); refresh(); }
                    case ALREADY_PENDING -> message(serverPlayer, "§eVocê já tem um teleporte em andamento.");
                    case COOLDOWN -> updateClock();
                }
            }
            return;
        }
        if (type != ClickType.PICKUP || button != 0) return;
        switch (slot) {
            case 45 -> { if (page > 0) { page--; refresh(); } }
            case 53 -> { if (page + 1 < pages()) { page++; refresh(); } }
            case 47 -> beginCreate(serverPlayer);
            case 49 -> serverPlayer.closeContainer();
            case 51 -> { deleting = !deleting; render(); }
            default -> { }
        }
    }

    private void beginCreate(ServerPlayer player) {
        if (!LobbyWorlds.isSurvivalEmerald(player.level())) {
            message(player, "§cCrie homes apenas no Survival Emerald."); return;
        }
        if (service.listHomes(owner).size() >= service.homeLimit(owner)) {
            message(player, "§cLimite de homes atingido."); refresh(); return;
        }
        newName = service.nextHomeName(owner);
        confirmation = Action.CREATE;
        render();
    }

    private void confirm(ServerPlayer player) {
        Action action = confirmation;
        confirmation = null; // A second packet cannot repeat a confirmed action.
        if (action != Action.CREATE && !service.exists(owner, selected)) {
            message(player, "§cEssa home não existe mais."); refresh(); return;
        }
        if (action == Action.DELETE) {
            message(player, service.deleteHome(owner, selected.name()) ? "§aHome excluída." : "§cHome não encontrada.");
        } else {
            if (action == Action.CREATE && service.listHomes(owner).stream().anyMatch(h -> h.name().equals(newName))) {
                message(player, "§eO nome já está em uso. Abra uma nova confirmação."); refresh(); return;
            }
            var result = service.setHome(player, action == Action.CREATE ? newName : selected.name());
            message(player, switch (result) {
                case CREATED -> "§aHome " + newName + " criada.";
                case UPDATED -> "§aPosição da home atualizada.";
                case LIMIT_REACHED -> "§cLimite de homes atingido.";
                case SURVIVAL_ONLY -> "§cSalve homes apenas no Survival Emerald.";
                case INVALID_NAME -> "§cNome de home inválido.";
            });
        }
        selected = null;
        refresh();
    }

    private static void message(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), false);
    }

    private static ItemStack icon(Item item, String name, String... lines) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        stack.set(DataComponents.LORE, new ItemLore(java.util.Arrays.stream(lines).map(Component::literal).map(c -> (Component) c).toList()));
        return stack;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
}
