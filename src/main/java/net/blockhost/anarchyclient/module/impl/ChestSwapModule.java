package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionConstraints;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.inventory.InventorySlots;
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

public final class ChestSwapModule extends Module {

    private static final int TIMEOUT_TICKS = 100;

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

    private SwapTarget target;
    private int ticksLeft;

    public ChestSwapModule() {
        super(
                "chest_swap",
                "Chest Swap",
                ModuleCategory.PLAYER,
                "Swaps between an elytra and the best chestplate in the chest slot, then turns itself off.");
    }

    @Override
    protected void onEnable() {
        this.target = null;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            // Enabled with no world (for example from config at startup): nothing to swap.
            this.enabled(false);
            return;
        }
        boolean wearingGlider = isGlider(player.getItemBySlot(EquipmentSlot.CHEST));
        if (this.target == null) {
            this.target = swapTarget(wearingGlider);
            this.ticksLeft = TIMEOUT_TICKS;
        }
        // The scheduler drops chains that are not renewed, so keep asking until the swap lands or times out.
        if (wearingGlider == (this.target == SwapTarget.GLIDER) || --this.ticksLeft <= 0) {
            this.enabled(false);
            return;
        }
        if (!InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        int inventorySlot = this.target == SwapTarget.CHESTPLATE
                ? findChestplateSlot(player.getInventory(), this.chestplate.value())
                : findGliderSlot(player.getInventory());
        InventorySlotRef source =
                inventorySlot >= 0 ? InventorySlots.storageSlot(inventorySlot).orElse(null) : null;
        if (source == null) {
            this.enabled(false);
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_EQUIPMENT,
                this.delay.value().intValue(),
                InventoryActionConstraints.cautiousPlayerInventory(),
                InventoryAction.pickupSwap(source, InventorySlots.armorSlot(EquipmentSlot.CHEST))));
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
