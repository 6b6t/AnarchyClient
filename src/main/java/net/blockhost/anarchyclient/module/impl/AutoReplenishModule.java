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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;

public final class AutoReplenishModule extends Module {

    private final NumberSetting threshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("threshold")
            .name("Threshold")
            .defaultValue(16.0)
            .min(1.0)
            .max(63.0)
            .step(1.0)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(8.0)
            .min(0.0)
            .max(80.0)
            .step(1.0)
            .build()));

    public AutoReplenishModule() {
        super("auto_replenish", "Auto Replenish", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        ReplenishMove move = findReplenishMove(player.getInventory(), this.threshold.value().intValue());
        if (move == null) {
            return;
        }
        InventorySlotRef source = InventorySlots.storageSlot(move.sourceSlot()).orElse(null);
        InventorySlotRef target = InventorySlots.storageSlot(move.hotbarSlot()).orElse(null);
        if (source == null || target == null) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_NORMAL,
                this.delay.value().intValue(),
                InventoryActionConstraints.cautiousPlayerInventory(),
                InventoryAction.pickupSwap(source, target)
        ));
    }

    static ReplenishMove findReplenishMove(final Inventory inventory, final int threshold) {
        int cappedThreshold = Math.max(1, threshold);
        for (int hotbarSlot = 0; hotbarSlot < Inventory.getSelectionSize(); hotbarSlot++) {
            ItemStack stack = inventory.getItem(hotbarSlot);
            if (!shouldReplenish(stack, cappedThreshold)) {
                continue;
            }
            OptionalInt source = findReplacementStack(inventory, stack, cappedThreshold);
            if (source.isPresent()) {
                return new ReplenishMove(hotbarSlot, source.orElseThrow());
            }
        }
        return null;
    }

    static boolean shouldReplenish(final ItemStack stack, final int threshold) {
        return !stack.isEmpty()
                && stack.isStackable()
                && stack.getCount() <= threshold
                && stack.getCount() < stack.getMaxStackSize();
    }

    private static OptionalInt findReplacementStack(final Inventory inventory, final ItemStack target, final int threshold) {
        int bestSlot = -1;
        int bestCount = threshold;
        for (int slot = Inventory.getSelectionSize(); slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()
                    || stack.getCount() <= bestCount
                    || !ItemStack.isSameItemSameComponents(target, stack)) {
                continue;
            }
            bestSlot = slot;
            bestCount = stack.getCount();
        }
        return bestSlot < 0 ? OptionalInt.empty() : OptionalInt.of(bestSlot);
    }

    record ReplenishMove(int hotbarSlot, int sourceSlot) {
    }
}
