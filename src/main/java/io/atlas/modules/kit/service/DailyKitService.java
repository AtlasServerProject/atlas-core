package io.atlas.modules.kit.service;

import io.atlas.modules.kit.repository.DailyKitRepository;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantments;
import io.atlas.modules.kit.menu.MintSelectionMenu;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DailyKitService {

    private static final List<String> MINTS = List.of(
            "lonely_mint", "adamant_mint", "naughty_mint", "brave_mint",
            "bold_mint", "impish_mint", "lax_mint", "relaxed_mint",
            "modest_mint", "mild_mint", "rash_mint", "quiet_mint",
            "calm_mint", "gentle_mint", "careful_mint", "sassy_mint",
            "timid_mint", "hasty_mint", "jolly_mint", "naive_mint",
            "serious_mint"
    );
    private static final List<String> EVOLUTION_STONES = List.of(
            "fire_stone", "water_stone", "thunder_stone", "leaf_stone",
            "moon_stone", "sun_stone", "shiny_stone", "dusk_stone",
            "dawn_stone", "ice_stone"
    );
    private static final List<String> STAT_BOTTLE_CAPS = List.of(
            "bottle_cap_hp", "bottle_cap_attack", "bottle_cap_defence",
            "bottle_cap_special_attack", "bottle_cap_special_defence", "bottle_cap_speed"
    );

    private final DailyKitRepository repository = new DailyKitRepository();
    private final Map<UUID, Integer> pendingMintChoices = new ConcurrentHashMap<>();

    public ClaimResult claim(ServerPlayer player) {
        return claim(player, KitType.DAILY);
    }

    public ClaimResult claim(ServerPlayer player, KitType kit) {
        if (!canClaim(player, kit)) {
            return ClaimResult.locked(kit);
        }

        Instant now = Instant.now();
        var lastClaim = repository.findLastClaim(player.getUUID(), kit.id());
        if (lastClaim.isPresent()) {
            Instant nextClaim = lastClaim.get().plus(kit.cooldown());
            if (nextClaim.isAfter(now)) {
                return ClaimResult.cooldown(kit, Duration.between(now, nextClaim));
            }
        }

        for (ItemStack reward : rewards(player, kit)) {
            give(player, reward);
        }
        if (kit.mintChoices() > 0) {
            pendingMintChoices.merge(player.getUUID(), kit.mintChoices(), Integer::sum);
        }
        repository.upsertClaim(player.getUUID(), kit.id());
        return ClaimResult.success(kit);
    }

    public boolean canClaim(ServerPlayer player, KitType kit) {
        return kit.requiredRanks().isEmpty()
                || playerRanks(player).stream().anyMatch(kit.requiredRanks()::contains);
    }

    public Optional<KitType> findBySlot(int slot) {
        for (KitType kit : KitType.values()) {
            if (kit.slot() == slot) {
                return Optional.of(kit);
            }
        }
        return Optional.empty();
    }

    public void openMintChoiceMenu(ServerPlayer player) {
        int pending = pendingMintChoices.getOrDefault(player.getUUID(), 0);
        if (pending <= 0) {
            return;
        }

        SimpleContainer container = new SimpleContainer(27);
        for (int i = 0; i < MINTS.size(); i++) {
            container.setItem(i, mintStack(MINTS.get(i), 1));
        }
        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new MintSelectionMenu(containerId, inventory, container, this),
                Component.literal("§dEscolha uma Mint §7(" + pending + " restante" + (pending > 1 ? "s" : "") + ")")
        ));
    }

    public boolean chooseMint(ServerPlayer player, int slot) {
        int pending = pendingMintChoices.getOrDefault(player.getUUID(), 0);
        if (pending <= 0 || slot < 0 || slot >= MINTS.size()) {
            return false;
        }

        give(player, mintStack(MINTS.get(slot), 1));
        if (pending <= 1) {
            pendingMintChoices.remove(player.getUUID());
            player.displayClientMessage(Component.literal("§aMint escolhida com sucesso."), false);
        } else {
            pendingMintChoices.put(player.getUUID(), pending - 1);
            player.displayClientMessage(Component.literal("§aMint escolhida. §eEscolhas restantes: §f" + (pending - 1)), false);
            player.getServer().execute(() -> openMintChoiceMenu(player));
        }
        return true;
    }

    public ItemStack createIcon(KitType kit) {
        ItemStack icon = stack("cobblemon", "gimmighoul_chest", 1, Items.CHEST);
        icon.set(DataComponents.CUSTOM_NAME, Component.literal(kit.color() + kit.displayName()));
        icon.set(DataComponents.LORE, new ItemLore(kit.lore()));
        icon.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return icon;
    }

    public String format(Duration duration) {
        long totalSeconds = Math.max(1L, duration.getSeconds());
        long days = totalSeconds / 86_400L;
        long hours = (totalSeconds % 86_400L) / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (days > 0) {
            return hours > 0 ? days + "d " + hours + "h" : days + "d";
        }
        if (hours > 0) {
            return minutes > 0 ? hours + "h " + minutes + "min" : hours + "h";
        }
        if (minutes > 0) {
            return seconds > 0 ? minutes + "min " + seconds + "s" : minutes + "min";
        }
        return seconds + "s";
    }

    private void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private List<ItemStack> rewards(ServerPlayer player, KitType kit) {
        List<ItemStack> rewards = new ArrayList<>();
        switch (kit) {
            case DAILY -> {
                rewards.add(stack("cobblemon", "poke_ball", 15, Items.AIR));
                rewards.add(stack("cobblemon", "great_ball", 10, Items.AIR));
                rewards.add(stack("cobblemon", "ultra_ball", 5, Items.AIR));
                rewards.add(createClaimShovel());
            }
            case WEEKLY -> {
                rewards.add(stack("cobblemon", "poke_ball", 16, Items.AIR));
                rewards.add(stack("cobblemon", "great_ball", 8, Items.AIR));
                rewards.add(stack("cobblemon", "ultra_ball", 4, Items.AIR));
                rewards.add(stack("cobblemon", "rare_candy", 8, Items.AIR));
            }
            case MONTHLY -> {
                rewards.add(enchantUnbreaking(player, namedTool(Items.IRON_PICKAXE, "§bPicareta Inicial")));
                rewards.add(enchantUnbreaking(player, namedTool(Items.IRON_AXE, "§bMachado Inicial")));
                rewards.add(enchantUnbreaking(player, namedTool(Items.IRON_SHOVEL, "§bPá Inicial")));
                rewards.add(enchantUnbreaking(player, namedTool(Items.IRON_HOE, "§bEnxada Inicial")));
                rewards.add(stack("cobblemon", "poke_ball", 32, Items.AIR));
                rewards.add(stack("cobblemon", "great_ball", 16, Items.AIR));
                rewards.add(stack("cobblemon", "ultra_ball", 8, Items.AIR));
                rewards.add(stack("cobblemon", "rare_candy", 16, Items.AIR));
                rewards.add(createClaimShovel());
            }
            case VIP -> {
                addBalls(rewards, 24, 16, 8);
                rewards.add(stack("cobblemon", "rare_candy", 2, Items.AIR));
                rewards.add(stack("cobblemon", "exp_candy_m", 2, Items.AIR));
                rewards.add(stack("cobblemon", "revive", 2, Items.AIR));
            }
            case VIP_PLUS -> {
                rewards.add(stack("cobblemon", "exp_share", 1, Items.AIR));
                rewards.add(stack("cobblemon", "exp_candy_xl", 2, Items.AIR));
                rewards.add(stack("cobblemon", "max_revive", 1, Items.AIR));
            }
            case VIP_PLUS_PLUS -> {
                rewards.add(stack("cobblemon", "exp_candy_xl", 2, Items.AIR));
                rewards.add(stack("cobblemon", "max_revive", 2, Items.AIR));
            }
            case VIP_WEEKLY -> {
                addBalls(rewards, 32, 16, 8);
                rewards.add(stack("cobblemon", "rare_candy", 12, Items.AIR));
                rewards.add(stack("cobblemon", "exp_candy_l", 8, Items.AIR));
                rewards.add(stack("cobblemon", "revive", 5, Items.AIR));
                rewards.add(randomMint(1));
                rewards.add(randomEvolutionStone(1));
            }
            case VIP_PLUS_WEEKLY -> {
                rewards.add(stack("cobblemon", "lucky_egg", 1, Items.AIR));
                rewards.add(stack("cobblemon", "ability_capsule", 1, Items.AIR));
                rewards.add(randomMint(1));
                rewards.add(randomMint(1));
            }
            case VIP_PLUS_PLUS_WEEKLY -> {
                rewards.add(stack("obc", "bottle_cap", 2, Items.AIR));
                rewards.add(stack("cobblemon", "ability_capsule", 3, Items.AIR));
                rewards.add(stack("cobblemon", "exp_candy_xl", 16, Items.AIR));
            }
            case VIP_MONTHLY -> {
                addBalls(rewards, 64, 32, 16);
                rewards.add(stack("cobblemon", "rare_candy", 32, Items.AIR));
                rewards.add(stack("cobblemon", "exp_candy_xl", 16, Items.AIR));
                rewards.add(stack("cobblemon", "revive", 10, Items.AIR));
                rewards.add(stack("cobblemon", "max_revive", 2, Items.AIR));
                rewards.add(randomMint(1));
                rewards.add(randomMint(1));
                rewards.add(randomEvolutionStone(1));
                rewards.add(randomEvolutionStone(1));
                rewards.add(stack("cobblemon", "lucky_egg", 1, Items.AIR));
                rewards.add(randomStatBottleCap(1));
            }
            case VIP_PLUS_MONTHLY -> {
                rewards.add(stack("cobblemon", "destiny_knot", 1, Items.AIR));
                rewards.add(stack("cobblemon", "everstone", 1, Items.AIR));
                rewards.add(stack("cobblemon", "lucky_egg", 2, Items.AIR));
                rewards.add(stack("cobblemon", "ability_capsule", 2, Items.AIR));
                rewards.add(stack("obc", "bottle_cap", 1, Items.AIR));
                rewards.add(stack("obc", "bottle_cap", 4, Items.AIR));
            }
            case VIP_PLUS_PLUS_MONTHLY -> {
                rewards.add(stack("cobblemon", "master_ball", 1, Items.AIR));
                rewards.add(stack("obc", "bottle_cap_gold", 1, Items.AIR));
                rewards.add(stack("cobblemon", "ability_patch", 2, Items.AIR));
                rewards.add(stack("cobblemon", "lucky_egg", 3, Items.AIR));
                rewards.add(stack("cobblemon", "destiny_knot", 2, Items.AIR));
                rewards.add(stack("cobblemon", "everstone", 2, Items.AIR));
                for (int i = 0; i < 5; i++) {
                    rewards.add(randomStatBottleCap(1));
                }
            }
        }
        return rewards;
    }

    private void addBalls(List<ItemStack> rewards, int pokeBalls, int greatBalls, int ultraBalls) {
        rewards.add(stack("cobblemon", "poke_ball", pokeBalls, Items.AIR));
        rewards.add(stack("cobblemon", "great_ball", greatBalls, Items.AIR));
        rewards.add(stack("cobblemon", "ultra_ball", ultraBalls, Items.AIR));
    }

    private ItemStack randomMint(int amount) {
        return mintStack(MINTS.get((int) (Math.random() * MINTS.size())), amount);
    }

    private ItemStack mintStack(String mint, int amount) {
        return stack("cobblemon", mint, amount, Items.AIR);
    }

    private ItemStack randomEvolutionStone(int amount) {
        return stack("cobblemon", EVOLUTION_STONES.get((int) (Math.random() * EVOLUTION_STONES.size())), amount, Items.AIR);
    }

    private ItemStack randomStatBottleCap(int amount) {
        return stack("obc", STAT_BOTTLE_CAPS.get((int) (Math.random() * STAT_BOTTLE_CAPS.size())), amount, Items.AIR);
    }

    private List<String> playerRanks(ServerPlayer player) {
        return io.atlas.modules.rank.RankModule.getRankService()
                .getPlayerRanks(player.getUUID())
                .stream()
                .map(rank -> rank.getIdentifier().toUpperCase(Locale.ROOT))
                .toList();
    }

    private ItemStack namedTool(Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Ferramenta inicial do Atlas."),
                Component.literal("§9Inquebrável III")
        )));
        return stack;
    }

    private ItemStack enchantUnbreaking(ServerPlayer player, ItemStack stack) {
        var lookup = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var unbreaking = lookup.getOrThrow(Enchantments.UNBREAKING);
        stack.enchant(unbreaking, 3);
        return stack;
    }

    private ItemStack createClaimShovel() {
        ItemStack shovel = new ItemStack(Items.GOLDEN_SHOVEL);
        shovel.set(DataComponents.CUSTOM_NAME, Component.literal("§6Pá de Claim do Atlas"));
        shovel.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Use para marcar os dois cantos"),
                Component.literal("§7da sua claim no Survival Emerald."),
                Component.literal("§8Kit Atlas")
        )));
        shovel.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return shovel;
    }

    private ItemStack stack(String namespace, String path, int amount, Item fallback) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(namespace, path));
        if (item == Items.AIR && fallback != Items.AIR) {
            item = fallback;
        }
        ItemStack stack = new ItemStack(item, amount);
        if (item == Items.AIR) {
            stack = new ItemStack(Items.BARRIER);
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("§cItem ausente: "
                    + namespace + ":" + path));
        }
        return stack;
    }

    public enum KitType {
        DAILY(
                "daily",
                "Kit Diário",
                "§a",
                2,
                Duration.ofHours(24),
                List.of(),
                List.of(
                        Component.literal("§715 Poké Bolas"),
                        Component.literal("§710 Super Bolas"),
                        Component.literal("§75 Ultra Bolas"),
                        Component.literal("§7Pá dourada de claims"),
                        Component.literal(""),
                        Component.literal("§aClique para resgatar.")
                ),
                0
        ),
        WEEKLY(
                "weekly",
                "Kit Semanal",
                "§b",
                4,
                Duration.ofDays(7),
                List.of(),
                List.of(
                        Component.literal("§716 Poké Bolas"),
                        Component.literal("§78 Super Bolas"),
                        Component.literal("§74 Ultra Bolas"),
                        Component.literal("§78 Doces Raros"),
                        Component.literal(""),
                        Component.literal("§bClique para resgatar.")
                ),
                0
        ),
        MONTHLY(
                "monthly",
                "Kit Mensal",
                "§6",
                6,
                Duration.ofDays(30),
                List.of(),
                List.of(
                        Component.literal("§7Ferramentas iniciais com Inquebrável III"),
                        Component.literal("§732 Poké Bolas, 16 Super Bolas, 8 Ultra Bolas"),
                        Component.literal("§716 Doces Raros"),
                        Component.literal("§7Pá dourada de claims"),
                        Component.literal(""),
                        Component.literal("§6Clique para resgatar.")
                ),
                0
        ),
        VIP(
                "vip",
                "Kit VIP Diário",
                "§d",
                11,
                Duration.ofHours(24),
                List.of("VIP", "VIP+", "VIPPLUS", "VIP++", "VIPPLUSPLUS", "SUP", "SUPPORT", "MOD", "MODERATOR", "ADM", "ADMIN", "DONO", "OWNER"),
                List.of(
                        Component.literal("§724 Poké Bolas"),
                        Component.literal("§716 Super Bolas"),
                        Component.literal("§78 Ultra Bolas"),
                        Component.literal("§72 Doces Raros, 2 Exp. Candy M"),
                        Component.literal("§72 Revives"),
                        Component.literal(""),
                        Component.literal("§dVIP ou superior.")
                ),
                0
        ),
        VIP_PLUS(
                "vip_plus",
                "Kit VIP ✦ Diário",
                "§5",
                13,
                Duration.ofHours(24),
                List.of("VIP+", "VIPPLUS", "VIP++", "VIPPLUSPLUS", "SUP", "SUPPORT", "MOD", "MODERATOR", "ADM", "ADMIN", "DONO", "OWNER"),
                List.of(
                        Component.literal("§71 Exp. Share"),
                        Component.literal("§72 Exp. Candy XL"),
                        Component.literal("§71 Max Revive"),
                        Component.literal(""),
                        Component.literal("§5VIP ✦ ou superior.")
                ),
                0
        ),
        VIP_PLUS_PLUS(
                "vip_plus_plus",
                "Kit VIP ✦✦ Diário",
                "§e",
                15,
                Duration.ofHours(24),
                List.of("VIP++", "VIPPLUSPLUS", "SUP", "SUPPORT", "MOD", "MODERATOR", "ADM", "ADMIN", "DONO", "OWNER"),
                List.of(
                        Component.literal("§72 Exp. Candy XL"),
                        Component.literal("§72 Max Revives"),
                        Component.literal("§71 Mint à escolha"),
                        Component.literal(""),
                        Component.literal("§eVIP ✦✦ ou superior.")
                ),
                1
        ),
        VIP_WEEKLY(
                "vip_weekly",
                "Kit VIP Semanal",
                "§d",
                20,
                Duration.ofDays(7),
                List.of("VIP", "VIP+", "VIPPLUS", "VIP++", "VIPPLUSPLUS", "SUP", "SUPPORT", "MOD", "MODERATOR", "ADM", "ADMIN", "DONO", "OWNER"),
                List.of(
                        Component.literal("§732 Poké Bolas, 16 Super Bolas, 8 Ultra Bolas"),
                        Component.literal("§712 Doces Raros, 8 Exp. Candy L"),
                        Component.literal("§75 Revives"),
                        Component.literal("§71 Mint aleatória"),
                        Component.literal("§71 Pedra de Evolução aleatória"),
                        Component.literal(""),
                        Component.literal("§dVIP ou superior.")
                ),
                0
        ),
        VIP_PLUS_WEEKLY(
                "vip_plus_weekly",
                "Kit VIP ✦ Semanal",
                "§5",
                22,
                Duration.ofDays(7),
                List.of("VIP+", "VIPPLUS", "VIP++", "VIPPLUSPLUS", "SUP", "SUPPORT", "MOD", "MODERATOR", "ADM", "ADMIN", "DONO", "OWNER"),
                List.of(
                        Component.literal("§71 Lucky Egg"),
                        Component.literal("§71 Ability Capsule"),
                        Component.literal("§72 Mints aleatórias"),
                        Component.literal(""),
                        Component.literal("§5VIP ✦ ou superior.")
                ),
                0
        ),
        VIP_PLUS_PLUS_WEEKLY(
                "vip_plus_plus_weekly",
                "Kit VIP ✦✦ Semanal",
                "§e",
                24,
                Duration.ofDays(7),
                List.of("VIP++", "VIPPLUSPLUS", "SUP", "SUPPORT", "MOD", "MODERATOR", "ADM", "ADMIN", "DONO", "OWNER"),
                List.of(
                        Component.literal("§72 Silver Bottle Caps"),
                        Component.literal("§73 Ability Capsules"),
                        Component.literal("§716 Exp. Candy XL"),
                        Component.literal(""),
                        Component.literal("§eVIP ✦✦ ou superior.")
                ),
                0
        ),
        VIP_MONTHLY(
                "vip_monthly",
                "Kit VIP Mensal",
                "§d",
                29,
                Duration.ofDays(30),
                List.of("VIP", "VIP+", "VIPPLUS", "VIP++", "VIPPLUSPLUS", "SUP", "SUPPORT", "MOD", "MODERATOR", "ADM", "ADMIN", "DONO", "OWNER"),
                List.of(
                        Component.literal("§764 Poké Bolas, 32 Super Bolas, 16 Ultra Bolas"),
                        Component.literal("§732 Doces Raros, 16 Exp. Candy XL"),
                        Component.literal("§710 Revives, 2 Max Revives"),
                        Component.literal("§72 Mints e 2 Pedras aleatórias"),
                        Component.literal("§71 Lucky Egg e 1 Bottle Cap de status"),
                        Component.literal(""),
                        Component.literal("§dVIP ou superior.")
                ),
                0
        ),
        VIP_PLUS_MONTHLY(
                "vip_plus_monthly",
                "Kit VIP ✦ Mensal",
                "§5",
                31,
                Duration.ofDays(30),
                List.of("VIP+", "VIPPLUS", "VIP++", "VIPPLUSPLUS", "SUP", "SUPPORT", "MOD", "MODERATOR", "ADM", "ADMIN", "DONO", "OWNER"),
                List.of(
                        Component.literal("§71 Destiny Knot e 1 Everstone"),
                        Component.literal("§72 Lucky Eggs"),
                        Component.literal("§72 Ability Capsules"),
                        Component.literal("§71 Silver Bottle Cap"),
                        Component.literal("§74 Bottle Caps"),
                        Component.literal(""),
                        Component.literal("§5VIP ✦ ou superior.")
                ),
                0
        ),
        VIP_PLUS_PLUS_MONTHLY(
                "vip_plus_plus_monthly",
                "Kit VIP ✦✦ Mensal",
                "§e",
                33,
                Duration.ofDays(30),
                List.of("VIP++", "VIPPLUSPLUS", "SUP", "SUPPORT", "MOD", "MODERATOR", "ADM", "ADMIN", "DONO", "OWNER"),
                List.of(
                        Component.literal("§71 Master Ball"),
                        Component.literal("§71 Golden Bottle Cap"),
                        Component.literal("§72 Ability Patches"),
                        Component.literal("§73 Lucky Eggs"),
                        Component.literal("§72 Destiny Knot e 2 Everstones"),
                        Component.literal("§75 Bottle Caps de status aleatórios"),
                        Component.literal("§75 Mints à escolha"),
                        Component.literal(""),
                        Component.literal("§eVIP ✦✦ ou superior.")
                ),
                5
        );

        private final String id;
        private final String displayName;
        private final String color;
        private final int slot;
        private final Duration cooldown;
        private final List<String> requiredRanks;
        private final List<Component> lore;
        private final int mintChoices;

        KitType(
                String id,
                String displayName,
                String color,
                int slot,
                Duration cooldown,
                List<String> requiredRanks,
                List<Component> lore,
                int mintChoices
        ) {
            this.id = id;
            this.displayName = displayName;
            this.color = color;
            this.slot = slot;
            this.cooldown = cooldown;
            this.requiredRanks = requiredRanks;
            this.lore = lore;
            this.mintChoices = mintChoices;
        }

        public String id() {
            return id;
        }

        public String displayName() {
            return displayName;
        }

        public String color() {
            return color;
        }

        public int slot() {
            return slot;
        }

        public Duration cooldown() {
            return cooldown;
        }

        public List<String> requiredRanks() {
            return requiredRanks;
        }

        public List<Component> lore() {
            return lore;
        }

        public int mintChoices() {
            return mintChoices;
        }

        public static Optional<KitType> byId(String id) {
            String normalized = id.toLowerCase(Locale.ROOT);
            for (KitType kit : values()) {
                if (kit.id.equals(normalized)) {
                    return Optional.of(kit);
                }
            }
            return Optional.empty();
        }
    }

    public record ClaimResult(boolean claimed, KitType kit, Duration remaining, boolean locked) {
        public static ClaimResult success(KitType kit) {
            return new ClaimResult(true, kit, Duration.ZERO, false);
        }

        public static ClaimResult cooldown(KitType kit, Duration remaining) {
            return new ClaimResult(false, kit, remaining, false);
        }

        public static ClaimResult locked(KitType kit) {
            return new ClaimResult(false, kit, Duration.ZERO, true);
        }
    }
}
