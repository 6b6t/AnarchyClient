package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionConstraints;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class ChestSwapModule extends Module {

    private final SelectSetting chestplate = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("chestplate")
            .name("Chestplate")
            .defaultValue("Best")
            .addAllOptions(List.of("Best", "Diamond", "Netherite"))
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(5.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));

    public ChestSwapModule() {
        super("chest_swap", "Chest Swap", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        int inventorySlot = isGlider(player.getItemBySlot(EquipmentSlot.CHEST))
                ? findChestplateSlot(player.getInventory(), this.chestplate.value())
                : findGliderSlot(player.getInventory());
        if (inventorySlot >= 0) {
            InventorySlotRef source = InventorySlots.storageSlot(inventorySlot).orElse(null);
            if (source != null) {
                InventoryActionScheduler.schedule(InventoryActionChain.single(
                        this.id(),
                        InventoryActionScheduler.PRIORITY_EQUIPMENT,
                        this.delay.value().intValue(),
                        InventoryActionConstraints.cautiousPlayerInventory(),
                        InventoryAction.pickupSwap(source, InventorySlots.armorSlot(EquipmentSlot.CHEST))
                ));
            }
        }
        this.enabled(false);
    }

    static boolean isGlider(final ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.has(DataComponents.GLIDER);
    }

    static SwapTarget swapTarget(final boolean wearingGlider) {
        return wearingGlider ? SwapTarget.CHESTPLATE : SwapTarget.GLIDER;
    }

    private static int findGliderSlot(final Inventory inventory) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (isGlider(inventory.getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private static int findChestplateSlot(final Inventory inventory, final String mode) {
        int bestSlot = -1;
        double bestScore = 0.0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isPreferredChestplate(stack, mode)) {
                continue;
            }
            double score = EquipmentScorer.armorScore(stack, EquipmentSlot.CHEST);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = slot;
            }
        }
        return bestSlot;
    }

    private static boolean isPreferredChestplate(final ItemStack stack, final String mode) {
        if (stack == null || stack.isEmpty() || isGlider(stack)) {
            return false;
        }
        return switch (mode) {
            case "Diamond" -> stack.is(Items.DIAMOND_CHESTPLATE);
            case "Netherite" -> stack.is(Items.NETHERITE_CHESTPLATE);
            default -> EquipmentScorer.armorScore(stack, EquipmentSlot.CHEST) > 0.0;
        };
    }

    enum SwapTarget {
        GLIDER,
        CHESTPLATE
    }
}
