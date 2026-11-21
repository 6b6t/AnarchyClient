package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;
import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.inventory.ItemUseControl;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

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
    private final BooleanSetting allowSoup = this.setting(BooleanSetting.from(
            BooleanSetting.builder().id("soup").name("Soup").defaultValue(true).build()));
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
    private InteractionHand eatingHand;

    public SmartEatModule() {
        super(
                "smart_eat",
                "Smart Eat",
                ModuleCategory.PLAYER,
                "Eats until the item is consumed when hunger or health is low, choosing food by quality and combat pressure.");
        this.healthThreshold.visibleWhen(() -> !"Hunger".equals(this.thresholdMode.value()));
        this.hungerThreshold.visibleWhen(() -> !"Health".equals(this.thresholdMode.value()));
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            return;
        }
        if (this.eatingHand != null) {
            // Keep holding the use until the item is consumed; vanilla would release it next tick.
            if (client.gui.screen() == null && player.isUsingItem() && player.getUsedItemHand() == this.eatingHand) {
                ItemUseControl.use(client, this.id(), this.eatingHand);
                return;
            }
            this.stopEating(client, player);
            this.cooldownTicks = this.delay.value().intValue();
        }
        if (client.gui.screen() != null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (player.isUsingItem() || (!this.allowDuringCombat.value() && player.getAttackStrengthScale(0.0F) < 1.0F)) {
            return;
        }

        Set<String> blacklistedItems = FoodSelection.parseBlacklist(this.blacklist.value());
        FoodSelection.FoodPriority priority = FoodSelection.FoodPriority.fromSetting(this.foodPriority.value());
        ConsumableChoice choice = this.pickChoice(player, blacklistedItems);
        if (choice == null) {
            return;
        }
        if (choice.kind() == ConsumableKind.SOUP
                && this.avoidClickable.value()
                && client.level != null
                && FoodSelection.isClickable(client.hitResult, client.level)) {
            return;
        }

        InteractionHand hand =
                FoodSelection.consumableHand(player.getMainHandItem(), player.getOffhandItem(), choice.predicate());
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
        if (hand == null) {
            return;
        }
        if (ItemUseControl.use(client, this.id(), hand)) {
            this.eatingHand = hand;
        } else {
            this.restorePreviousSlot(player);
            this.cooldownTicks = this.delay.value().intValue();
        }
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            this.stopEating(client, client.player);
        }
        this.eatingHand = null;
        this.previousSlot = -1;
        this.cooldownTicks = 0;
    }

    private void stopEating(final Minecraft client, final LocalPlayer player) {
        ItemUseControl.release(client, this.id());
        this.eatingHand = null;
        this.restorePreviousSlot(player);
    }

    private void restorePreviousSlot(final LocalPlayer player) {
        if (this.previousSlot >= 0 && this.restoreSlot.value()) {
            InventoryActions.selectHotbarSlot(player, this.previousSlot);
        }
        this.previousSlot = -1;
    }

    private ConsumableChoice pickChoice(final LocalPlayer player, final Set<String> blacklist) {
        float health = player.getHealth();
        // Vanilla refuses most food at full hunger; picking it anyway would swap slots back and forth.
        if (this.allowSoup.value() && health <= this.soupHealth.value() && player.canEat(false)) {
            return new ConsumableChoice(ConsumableKind.SOUP, FoodSelection::isSoup);
        }
        if (this.allowGapples.value() && health <= this.gappleHealth.value()) {
            return new ConsumableChoice(ConsumableKind.GAPPLE, FoodSelection::isGapple);
        }
        if (FoodSelection.shouldEat(
                player.getFoodData().getFoodLevel(),
                health,
                this.hungerThreshold.value(),
                this.healthThreshold.value(),
                FoodSelection.ThresholdMode.fromSetting(this.thresholdMode.value()))) {
            return new ConsumableChoice(
                    ConsumableKind.FOOD,
                    stack -> FoodSelection.isFood(stack, false, blacklist)
                            && FoodSelection.canEatNow(player.canEat(false), stack));
        }
        return null;
    }

    private OptionalInt findHotbarSlot(
            final Inventory inventory, final ConsumableChoice choice, final FoodSelection.FoodPriority priority) {
        return switch (choice.kind()) {
            case SOUP -> FoodSelection.findSoupSlot(inventory, 0, Inventory.getSelectionSize() - 1);
            case GAPPLE ->
                FoodSelection.bestGappleSlot(
                        inventory, 0, Inventory.getSelectionSize() - 1, this.preferEnchantedGapples.value());
            case FOOD ->
                FoodSelection.findBestFoodSlot(
                        inventory, 0, Inventory.getSelectionSize() - 1, choice.predicate(), priority);
        };
    }

    private OptionalInt findInventorySlot(
            final Inventory inventory, final ConsumableChoice choice, final FoodSelection.FoodPriority priority) {
        return switch (choice.kind()) {
            case SOUP ->
                FoodSelection.findSoupSlot(inventory, Inventory.getSelectionSize(), Inventory.INVENTORY_SIZE - 1);
            case GAPPLE ->
                FoodSelection.bestGappleSlot(
                        inventory,
                        Inventory.getSelectionSize(),
                        Inventory.INVENTORY_SIZE - 1,
                        this.preferEnchantedGapples.value());
            case FOOD ->
                FoodSelection.findBestFoodSlot(
                        inventory,
                        Inventory.getSelectionSize(),
                        Inventory.INVENTORY_SIZE - 1,
                        choice.predicate(),
                        priority);
        };
    }

    private void queueInventoryMove(
            final Minecraft client,
            final LocalPlayer player,
            final ConsumableChoice choice,
            final FoodSelection.FoodPriority priority) {
        if (!InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        OptionalInt sourceSlot = this.findInventorySlot(player.getInventory(), choice, priority);
        OptionalInt targetSlot = FoodSelection.findEmptyHotbarSlot(player.getInventory());
        if (sourceSlot.isEmpty() || targetSlot.isEmpty()) {
            return;
        }
        InventorySlotRef source =
                InventorySlots.storageSlot(sourceSlot.orElseThrow()).orElse(null);
        InventorySlotRef target =
                InventorySlots.storageSlot(targetSlot.orElseThrow()).orElse(null);
        if (source == null || target == null) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(), InventoryActionScheduler.PRIORITY_LIFE, 2, InventoryAction.pickupSwap(source, target)));
    }

    private enum ConsumableKind {
        FOOD,
        GAPPLE,
        SOUP
    }

    private record ConsumableChoice(ConsumableKind kind, Predicate<ItemStack> predicate) {}
}
