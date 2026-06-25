package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionConstraints;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class InventoryCleanerModule extends Module {

    private static final String DEFAULT_JUNK_ITEMS = "rotten_flesh, poisonous_potato, spider_eye";

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Junk Only")
            .addAllOptions(List.of("Junk Only", "Strict"))
            .build()));
    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(8.0)
            .min(0.0)
            .max(80.0)
            .step(1.0)
            .build()));
    private final StringSetting junkItems = this.setting(StringSetting.from(StringSetting.builder()
            .id("junk_items")
            .name("Junk")
            .defaultValue(DEFAULT_JUNK_ITEMS)
            .build()));
    private final BooleanSetting keepWeapons = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_weapons")
            .name("Weapons")
            .defaultValue(true)
            .build()));
    private final BooleanSetting keepTools = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_tools")
            .name("Tools")
            .defaultValue(true)
            .build()));
    private final BooleanSetting keepArmor = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_armor")
            .name("Armor")
            .defaultValue(true)
            .build()));
    private final BooleanSetting keepFood = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_food")
            .name("Food")
            .defaultValue(true)
            .build()));
    private final BooleanSetting keepBlocks = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_blocks")
            .name("Blocks")
            .defaultValue(true)
            .build()));
    private final BooleanSetting keepTotems = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_totems")
            .name("Totems")
            .defaultValue(true)
            .build()));
    private final BooleanSetting keepPearls = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_pearls")
            .name("Pearls")
            .defaultValue(true)
            .build()));

    public InventoryCleanerModule() {
        super("inventory_cleaner", "Inventory Cleaner", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        List<ItemStack> stacks = inventoryStacks(player.getInventory());
        CleanerOptions options = new CleanerOptions(
                "Strict".equals(this.mode.value()),
                ItemScan.parseItems(this.junkItems.value()),
                this.keepWeapons.value(),
                this.keepTools.value(),
                this.keepArmor.value(),
                this.keepFood.value(),
                this.keepBlocks.value(),
                this.keepTotems.value(),
                this.keepPearls.value()
        );
        int inventorySlot = findDropSlot(stacks, player.getInventory().getSelectedSlot(), options);
        int menuSlot = InventorySlots.toInventoryMenuSlot(inventorySlot);
        if (menuSlot < 0) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_NORMAL,
                this.delayTicks.value().intValue(),
                InventoryActionConstraints.cautiousPlayerInventory(),
                InventoryAction.dropSlot(menuSlot, true)
        ));
    }

    static int findDropSlot(final List<ItemStack> stacks, final int selectedSlot, final CleanerOptions options) {
        Set<Integer> keepSlots = keepSlots(stacks, options);
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (slot == selectedSlot || stack.isEmpty()) {
                continue;
            }
            if (isConfiguredJunk(stack, options.junkItems())) {
                return slot;
            }
            if (options.strict() && !keepSlots.contains(slot)) {
                return slot;
            }
        }
        return -1;
    }

    static boolean isConfiguredJunk(final ItemStack stack, final Set<Item> junkItems) {
        return !stack.isEmpty() && junkItems.contains(stack.getItem());
    }

    static Set<Item> defaultJunkItems() {
        return ItemScan.parseItems(DEFAULT_JUNK_ITEMS);
    }

    static CleanerOptions defaultOptions() {
        return new CleanerOptions(false, defaultJunkItems(), true, true, true, true, true, true, true);
    }

    private static Set<Integer> keepSlots(final List<ItemStack> stacks, final CleanerOptions options) {
        Set<Integer> keep = new HashSet<>();
        if (options.keepWeapons()) {
            keepBestWeapon(stacks, keep);
        }
        if (options.keepTools()) {
            keepBestTool(stacks, keep, ToolKind.PICKAXE);
            keepBestTool(stacks, keep, ToolKind.AXE);
            keepBestTool(stacks, keep, ToolKind.SHOVEL);
            keepBestTool(stacks, keep, ToolKind.HOE);
        }
        if (options.keepArmor()) {
            keepBestArmor(stacks, keep, EquipmentSlot.HEAD);
            keepBestArmor(stacks, keep, EquipmentSlot.CHEST);
            keepBestArmor(stacks, keep, EquipmentSlot.LEGS);
            keepBestArmor(stacks, keep, EquipmentSlot.FEET);
        }
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (options.keepFood() && stack.has(DataComponents.FOOD)
                    || options.keepBlocks() && stack.getItem() instanceof BlockItem
                    || options.keepTotems() && stack.is(Items.TOTEM_OF_UNDYING)
                    || options.keepPearls() && stack.is(Items.ENDER_PEARL)) {
                keep.add(slot);
            }
        }
        return keep;
    }

    private static void keepBestWeapon(final List<ItemStack> stacks, final Set<Integer> keep) {
        int bestSlot = -1;
        double bestScore = 0.0;
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (!AutoClickerModule.isWeapon(stack)) {
                continue;
            }
            double score = EquipmentScorer.weaponScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        if (bestSlot >= 0) {
            keep.add(bestSlot);
        }
    }

    private static void keepBestTool(final List<ItemStack> stacks, final Set<Integer> keep, final ToolKind kind) {
        int bestSlot = -1;
        double bestScore = 0.0;
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            if (!kind.matches(stack)) {
                continue;
            }
            double score = EquipmentScorer.durabilityRatio(stack) + (stack.has(DataComponents.TOOL) ? 1.0 : 0.0);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        if (bestSlot >= 0) {
            keep.add(bestSlot);
        }
    }

    private static void keepBestArmor(final List<ItemStack> stacks, final Set<Integer> keep, final EquipmentSlot slotType) {
        int bestSlot = -1;
        double bestScore = 0.0;
        for (int slot = 0; slot < stacks.size(); slot++) {
            ItemStack stack = stacks.get(slot);
            double score = EquipmentScorer.armorScore(stack, slotType);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        if (bestSlot >= 0) {
            keep.add(bestSlot);
        }
    }

    private static List<ItemStack> inventoryStacks(final Inventory inventory) {
        List<ItemStack> stacks = new ArrayList<>(Inventory.INVENTORY_SIZE);
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            stacks.add(inventory.getItem(slot));
        }
        return stacks;
    }

    private enum ToolKind {
        PICKAXE,
        AXE,
        SHOVEL,
        HOE;

        boolean matches(final ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            return switch (this) {
                case PICKAXE -> stack.is(ItemTags.PICKAXES);
                case AXE -> stack.is(ItemTags.AXES) && !AutoClickerModule.isWeapon(stack);
                case SHOVEL -> stack.is(ItemTags.SHOVELS);
                case HOE -> stack.is(ItemTags.HOES);
            };
        }
    }

    record CleanerOptions(boolean strict, Set<Item> junkItems, boolean keepWeapons, boolean keepTools,
                          boolean keepArmor, boolean keepFood, boolean keepBlocks, boolean keepTotems,
                          boolean keepPearls) {

        CleanerOptions {
            junkItems = Set.copyOf(junkItems);
        }
    }
}
