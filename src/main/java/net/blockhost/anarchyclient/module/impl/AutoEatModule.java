package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

public final class AutoEatModule extends Module {

    private final SelectSetting thresholdMode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("threshold_mode")
            .name("Threshold")
            .defaultValue("Any")
            .addAllOptions(List.of("Any", "Both", "Health", "Hunger"))
            .build()));
    private final NumberSetting hungerThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("hunger_threshold")
            .name("Hunger")
            .defaultValue(14.0)
            .min(1.0)
            .max(19.0)
            .step(1.0)
            .build()));
    private final NumberSetting healthThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health_threshold")
            .name("Health")
            .defaultValue(10.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final BooleanSetting allowDuringCombat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("allow_during_combat")
            .name("In Combat")
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
            .defaultValue(false)
            .build()));
    private final BooleanSetting restoreSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore_slot")
            .name("Restore")
            .defaultValue(true)
            .build()));
    private final BooleanSetting useGoldenApples = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("use_golden_apples")
            .name("Gapples")
            .defaultValue(false)
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
    private int previousSlot = -1;

    public AutoEatModule() {
        super("auto_eat", "Auto Eat", ModuleCategory.PLAYER);
        this.healthThreshold.visibleWhen(() -> !"Hunger".equals(this.thresholdMode.value()));
        this.hungerThreshold.visibleWhen(() -> !"Health".equals(this.thresholdMode.value()));
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gui.screen() != null || client.gameMode == null) {
            return;
        }
        if (this.previousSlot >= 0 && !player.isUsingItem()) {
            if (this.restoreSlot.value()) {
                InventoryActions.selectHotbarSlot(player, this.previousSlot);
            }
            this.previousSlot = -1;
        }
        if (player.isUsingItem() || !shouldEat(
                player.getFoodData().getFoodLevel(),
                player.getHealth(),
                this.hungerThreshold.value(),
                this.healthThreshold.value(),
                ThresholdMode.fromSetting(this.thresholdMode.value())
        )) {
            return;
        }
        if (!this.allowDuringCombat.value() && player.getAttackStrengthScale(0.0F) < 1.0F) {
            return;
        }
        Set<String> blacklistedItems = parseBlacklist(this.blacklist.value());
        FoodPriority priority = FoodPriority.fromSetting(this.foodPriority.value());
        InteractionHand hand = foodHand(player.getMainHandItem(), player.getOffhandItem(), this.useGoldenApples.value(), blacklistedItems);
        if (hand == null && this.selectHotbarFood.value()) {
            int selectedSlot = player.getInventory().getSelectedSlot();
            OptionalInt foodSlot = findBestFoodSlot(player.getInventory(), 0, Inventory.getSelectionSize() - 1,
                    this.useGoldenApples.value(), blacklistedItems, priority);
            if (foodSlot.isPresent() && foodSlot.orElseThrow() != selectedSlot) {
                this.previousSlot = selectedSlot;
                InventoryActions.selectHotbarSlot(player, foodSlot.orElseThrow());
                hand = InteractionHand.MAIN_HAND;
            } else if (foodSlot.isEmpty() && this.searchInventory.value()) {
                this.queueInventoryFoodMove(client, player, blacklistedItems, priority);
                return;
            }
        }
        if (hand != null) {
            client.gameMode.useItem(player, hand);
        }
    }

    static boolean shouldEat(final int hunger, final float health, final double hungerThreshold, final double healthThreshold) {
        return shouldEat(hunger, health, hungerThreshold, healthThreshold, ThresholdMode.ANY);
    }

    static boolean shouldEat(final int hunger, final float health, final double hungerThreshold,
                             final double healthThreshold, final ThresholdMode mode) {
        boolean hungerLow = hunger <= hungerThreshold;
        boolean healthLow = health <= healthThreshold;
        return mode.test(healthLow, hungerLow);
    }

    private void queueInventoryFoodMove(final Minecraft client, final LocalPlayer player, final Set<String> blacklist,
                                        final FoodPriority priority) {
        if (!InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        OptionalInt foodSlot = findBestFoodSlot(player.getInventory(), Inventory.getSelectionSize(),
                Inventory.INVENTORY_SIZE - 1, this.useGoldenApples.value(), blacklist, priority);
        if (foodSlot.isEmpty()) {
            return;
        }
        OptionalInt emptyHotbarSlot = findEmptyHotbarSlot(player.getInventory());
        if (emptyHotbarSlot.isEmpty()) {
            return;
        }
        InventorySlotRef source = InventorySlots.storageSlot(foodSlot.orElseThrow()).orElse(null);
        InventorySlotRef target = InventorySlots.storageSlot(emptyHotbarSlot.orElseThrow()).orElse(null);
        if (source == null || target == null) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_NORMAL,
                2,
                InventoryAction.pickupSwap(source, target)
        ));
    }

    private static OptionalInt findEmptyHotbarSlot(final Inventory inventory) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    private static InteractionHand foodHand(final ItemStack mainHand, final ItemStack offHand, final boolean allowGoldenApples,
                                            final Set<String> blacklist) {
        if (isFood(mainHand, allowGoldenApples, blacklist)) {
            return InteractionHand.MAIN_HAND;
        }
        if (isFood(offHand, allowGoldenApples, blacklist)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean isFood(final ItemStack stack, final boolean allowGoldenApples) {
        return isFood(stack, allowGoldenApples, Set.of());
    }

    private static boolean isFood(final ItemStack stack, final boolean allowGoldenApples, final Set<String> blacklist) {
        if (!stack.has(DataComponents.FOOD)) {
            return false;
        }
        if (!allowGoldenApples && (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE))) {
            return false;
        }
        return !isBlacklisted(stack, blacklist);
    }

    private static OptionalInt findBestFoodSlot(final Inventory inventory, final int start, final int end,
                                                final boolean allowGoldenApples, final Set<String> blacklist,
                                                final FoodPriority priority) {
        int bestSlot = -1;
        double bestScore = -1;
        for (int slot = start; slot <= end; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isFood(stack, allowGoldenApples, blacklist)) {
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
        return java.util.Arrays.stream(value.split("[,;|]"))
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
}
