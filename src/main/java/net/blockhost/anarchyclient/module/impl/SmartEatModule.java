package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class SmartEatModule extends Module {

    private final SelectSetting thresholdMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("threshold_mode")
            .name("Threshold")
            .defaultValue("Any")
            .addAllOptions(List.of("Any", "Both", "Health", "Hunger"))
            .build()));
    private final NumberSetting healthThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health_threshold")
            .name("Health")
            .defaultValue(10.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final NumberSetting hungerThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("hunger_threshold")
            .name("Hunger")
            .defaultValue(14.0)
            .min(1.0)
            .max(19.0)
            .step(1.0)
            .build()));
    private final NumberSetting gappleHealth = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("gapple_health")
            .name("Gap HP")
            .defaultValue(8.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final NumberSetting soupHealth = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("soup_health")
            .name("Soup HP")
            .defaultValue(14.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final BooleanSetting allowGapples = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("gapples")
            .name("Gapples")
            .defaultValue(true)
            .build()));
    private final BooleanSetting preferEnchantedGapples = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("prefer_enchanted_gapples")
            .name("Enchanted")
            .defaultValue(true)
            .build()));
    private final BooleanSetting allowSoup = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("soup")
            .name("Soup")
            .defaultValue(true)
            .build()));
    private final BooleanSetting allowDuringCombat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("allow_combat")
            .name("Combat")
            .defaultValue(false)
            .build()));
    private final BooleanSetting selectHotbarFood = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("select_hotbar_food")
            .name("Hotbar")
            .defaultValue(true)
            .build()));
    private final BooleanSetting searchInventory = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("search_inventory")
            .name("Inventory")
            .defaultValue(true)
            .build()));
    private final BooleanSetting restoreSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore_slot")
            .name("Restore")
            .defaultValue(true)
            .build()));
    private final BooleanSetting avoidClickable = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("avoid_clickable")
            .name("Safe Use")
            .defaultValue(true)
            .build()));
    private final SelectSetting foodPriority = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("food_priority")
            .name("Priority")
            .defaultValue("Saturation")
            .addAllOptions(List.of("Saturation", "Hunger", "Combined"))
            .build()));
    private final StringSetting blacklist = this.setting(StringSetting.from(StringSetting.builder()
            .id("blacklist")
            .name("Blacklist")
            .defaultValue("poisonous_potato, pufferfish, chicken, rotten_flesh, spider_eye, suspicious_stew")
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(2.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));

    private int previousSlot = -1;
    private int cooldownTicks;

    public SmartEatModule() {
        super("smart_eat", "Smart Eat", ModuleCategory.PLAYER);
        this.healthThreshold.visibleWhen(() -> !"Hunger".equals(this.thresholdMode.value()));
        this.hungerThreshold.visibleWhen(() -> !"Health".equals(this.thresholdMode.value()));
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.previousSlot >= 0 && !player.isUsingItem()) {
            if (this.restoreSlot.value()) {
                InventoryActions.selectHotbarSlot(player, this.previousSlot);
            }
            this.previousSlot = -1;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (player.isUsingItem() || !this.allowDuringCombat.value() && player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }

        Set<String> blacklistedItems = parseBlacklist(this.blacklist.value());
        FoodPriority priority = FoodPriority.fromSetting(this.foodPriority.value());
        ConsumableChoice choice = this.pickChoice(player, blacklistedItems);
        if (choice == null) {
            return;
        }
        if (choice.kind() == ConsumableKind.SOUP
                && this.avoidClickable.value()
                && client.level != null
                && isClickable(client.hitResult, client.level)) {
            return;
        }

        InteractionHand hand = consumableHand(player.getMainHandItem(), player.getOffhandItem(), choice.predicate());
        if (hand == null && this.selectHotbarFood.value()) {
            OptionalInt slot = this.findHotbarSlot(player.getInventory(), choice, priority);
            if (slot.isPresent()) {
                int selectedSlot = player.getInventory().getSelectedSlot();
                if (slot.orElseThrow() != selectedSlot) {
                    this.previousSlot = selectedSlot;
                    InventoryActions.selectHotbarSlot(player, slot.orElseThrow());
                }
                hand = InteractionHand.MAIN_HAND;
            } else if (this.searchInventory.value()) {
                this.queueInventoryMove(client, player, choice, priority);
                return;
            }
        }
        if (hand != null) {
            client.gameMode.useItem(player, hand);
            this.cooldownTicks = this.delay.value().intValue();
        }
    }

    @Override
    protected void onDisable() {
        this.previousSlot = -1;
        this.cooldownTicks = 0;
    }

    private ConsumableChoice pickChoice(final LocalPlayer player, final Set<String> blacklist) {
        float health = player.getHealth();
        if (this.allowSoup.value() && health <= this.soupHealth.value()) {
            return new ConsumableChoice(ConsumableKind.SOUP, SmartEatModule::isSoup);
        }
        if (this.allowGapples.value() && health <= this.gappleHealth.value()) {
            return new ConsumableChoice(ConsumableKind.GAPPLE, SmartEatModule::isGapple);
        }
        if (shouldEat(
                player.getFoodData().getFoodLevel(),
                health,
                this.hungerThreshold.value(),
                this.healthThreshold.value(),
                ThresholdMode.fromSetting(this.thresholdMode.value())
        )) {
            return new ConsumableChoice(ConsumableKind.FOOD, stack -> isFood(stack, false, blacklist));
        }
        return null;
    }

    private OptionalInt findHotbarSlot(final Inventory inventory, final ConsumableChoice choice,
                                       final FoodPriority priority) {
        return switch (choice.kind()) {
            case SOUP -> findSoupSlot(inventory, 0, Inventory.getSelectionSize() - 1);
            case GAPPLE -> bestGappleSlot(inventory, 0, Inventory.getSelectionSize() - 1,
                    this.preferEnchantedGapples.value());
            case FOOD -> findBestFoodSlot(inventory, 0, Inventory.getSelectionSize() - 1,
                    choice.predicate(), priority);
        };
    }

    private OptionalInt findInventorySlot(final Inventory inventory, final ConsumableChoice choice,
                                          final FoodPriority priority) {
        return switch (choice.kind()) {
            case SOUP -> findSoupSlot(inventory, Inventory.getSelectionSize(), Inventory.INVENTORY_SIZE - 1);
            case GAPPLE -> bestGappleSlot(inventory, Inventory.getSelectionSize(), Inventory.INVENTORY_SIZE - 1,
                    this.preferEnchantedGapples.value());
            case FOOD -> findBestFoodSlot(inventory, Inventory.getSelectionSize(), Inventory.INVENTORY_SIZE - 1,
                    choice.predicate(), priority);
        };
    }

    private void queueInventoryMove(final Minecraft client, final LocalPlayer player, final ConsumableChoice choice,
                                    final FoodPriority priority) {
        if (!InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        OptionalInt sourceSlot = this.findInventorySlot(player.getInventory(), choice, priority);
        OptionalInt targetSlot = findEmptyHotbarSlot(player.getInventory());
        if (sourceSlot.isEmpty() || targetSlot.isEmpty()) {
            return;
        }
        InventorySlotRef source = InventorySlots.storageSlot(sourceSlot.orElseThrow()).orElse(null);
        InventorySlotRef target = InventorySlots.storageSlot(targetSlot.orElseThrow()).orElse(null);
        if (source == null || target == null) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_LIFE,
                2,
                InventoryAction.pickupSwap(source, target)
        ));
    }

    static boolean shouldEat(final int hunger, final float health, final double hungerThreshold,
                             final double healthThreshold) {
        return shouldEat(hunger, health, hungerThreshold, healthThreshold, ThresholdMode.ANY);
    }

    static boolean shouldEat(final int hunger, final float health, final double hungerThreshold,
                             final double healthThreshold, final ThresholdMode mode) {
        boolean hungerLow = hunger <= hungerThreshold;
        boolean healthLow = health <= healthThreshold;
        return mode.test(healthLow, hungerLow);
    }

    static OptionalInt bestGappleSlot(final Inventory inventory, final boolean preferEnchanted) {
        return bestGappleSlot(inventory, 0, Inventory.getSelectionSize() - 1, preferEnchanted);
    }

    private static OptionalInt bestGappleSlot(final Inventory inventory, final int start, final int end,
                                              final boolean preferEnchanted) {
        OptionalInt enchanted = findSlot(inventory, start, end, stack -> stack.is(Items.ENCHANTED_GOLDEN_APPLE));
        OptionalInt golden = findSlot(inventory, start, end, stack -> stack.is(Items.GOLDEN_APPLE));
        if (preferEnchanted && enchanted.isPresent()) {
            return enchanted;
        }
        return golden.isPresent() ? golden : enchanted;
    }

    static OptionalInt findSoupSlot(final Inventory inventory, final int start, final int end) {
        return findSlot(inventory, start, end, SmartEatModule::isSoup);
    }

    static InteractionHand soupHand(final ItemStack mainHand, final ItemStack offHand) {
        return consumableHand(mainHand, offHand, SmartEatModule::isSoup);
    }

    static boolean isSoup(final ItemStack stack) {
        return !stack.isEmpty() && isSoupItemId(stack.getItem().builtInRegistryHolder().key().identifier().getPath());
    }

    static boolean isSoupItemId(final String itemId) {
        return "mushroom_stew".equals(itemId)
                || "rabbit_stew".equals(itemId)
                || "beetroot_soup".equals(itemId)
                || "suspicious_stew".equals(itemId);
    }

    static boolean isClickable(final HitResult hitResult, final Level level) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            return entity instanceof Villager || entity instanceof TamableAnimal;
        }
        if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHitResult.getBlockPos();
            Block block = level.getBlockState(pos).getBlock();
            return block instanceof BaseEntityBlock || block instanceof CraftingTableBlock;
        }
        return false;
    }

    static double foodScore(final ItemStack stack, final FoodPriority priority) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) {
            return 0.0;
        }
        return foodScore(food, priority);
    }

    static double foodScore(final FoodProperties food, final FoodPriority priority) {
        return switch (priority) {
            case HUNGER -> food.nutrition();
            case SATURATION -> food.saturation();
            case COMBINED -> food.nutrition() + food.saturation();
        };
    }

    static Set<String> parseBlacklist(final String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(value.split("[,;|\\s]+"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> token.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
    }

    static boolean isBlacklisted(final ItemStack stack, final Set<String> blacklist) {
        return isBlacklisted(stack.getItem().builtInRegistryHolder().key().identifier().toString(), blacklist);
    }

    static boolean isBlacklisted(final String itemId, final Set<String> blacklist) {
        String id = itemId.toLowerCase(Locale.ROOT);
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return blacklist.contains(path) || blacklist.contains(id);
    }

    private static OptionalInt findBestFoodSlot(final Inventory inventory, final int start, final int end,
                                                final Predicate<ItemStack> predicate, final FoodPriority priority) {
        int bestSlot = -1;
        double bestScore = -1;
        for (int slot = start; slot <= end; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!predicate.test(stack)) {
                continue;
            }
            double score = foodScore(stack, priority);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot < 0 ? OptionalInt.empty() : OptionalInt.of(bestSlot);
    }

    private static OptionalInt findSlot(final Inventory inventory, final int start, final int end,
                                        final Predicate<ItemStack> predicate) {
        for (int slot = start; slot <= end; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    private static OptionalInt findEmptyHotbarSlot(final Inventory inventory) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    private static InteractionHand consumableHand(final ItemStack mainHand, final ItemStack offHand,
                                                  final Predicate<ItemStack> predicate) {
        if (predicate.test(mainHand)) {
            return InteractionHand.MAIN_HAND;
        }
        if (predicate.test(offHand)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isFood(final ItemStack stack, final boolean allowGoldenApples, final Set<String> blacklist) {
        if (!stack.has(DataComponents.FOOD)) {
            return false;
        }
        if (!allowGoldenApples && isGapple(stack)) {
            return false;
        }
        return !isBlacklisted(stack, blacklist);
    }

    private static boolean isGapple(final ItemStack stack) {
        return stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    enum ThresholdMode {
        HEALTH,
        HUNGER,
        ANY,
        BOTH;

        private boolean test(final boolean healthLow, final boolean hungerLow) {
            return switch (this) {
                case HEALTH -> healthLow;
                case HUNGER -> hungerLow;
                case BOTH -> healthLow && hungerLow;
                case ANY -> healthLow || hungerLow;
            };
        }

        private static ThresholdMode fromSetting(final String value) {
            return switch (value) {
                case "Health" -> HEALTH;
                case "Hunger" -> HUNGER;
                case "Both" -> BOTH;
                default -> ANY;
            };
        }
    }

    enum FoodPriority {
        SATURATION,
        HUNGER,
        COMBINED;

        private static FoodPriority fromSetting(final String value) {
            return switch (value) {
                case "Hunger" -> HUNGER;
                case "Combined" -> COMBINED;
                default -> SATURATION;
            };
        }
    }

    private enum ConsumableKind {
        FOOD,
        GAPPLE,
        SOUP
    }

    private record ConsumableChoice(ConsumableKind kind, Predicate<ItemStack> predicate) {
    }
}
