package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionConstraints;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;

public final class InventoryTweaksModule extends Module {

    private final BooleanSetting refillSelected = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("refill_selected")
            .name("Refill Selected")
            .defaultValue(true)
            .build()));
    private final NumberSetting refillThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("refill_threshold")
            .name("Refill At")
            .defaultValue(16.0)
            .min(1.0)
            .max(63.0)
            .step(1.0)
            .build()));
    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(6.0)
            .min(0.0)
            .max(80.0)
            .step(1.0)
            .build()));

    public InventoryTweaksModule() {
        super("inventory_tweaks", "Inventory Tweaks", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (!this.refillSelected.value()
                || player == null
                || !InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        RefillMove move = selectedRefillMove(player.getInventory(), this.refillThreshold.value().intValue());
        if (move == null) {
            return;
        }
        InventorySlotRef source = InventorySlots.storageSlot(move.sourceSlot()).orElse(null);
        InventorySlotRef target = InventorySlots.storageSlot(move.targetSlot()).orElse(null);
        if (source == null || target == null) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_NORMAL,
                this.delayTicks.value().intValue(),
                InventoryActionConstraints.cautiousPlayerInventory(),
                InventoryAction.pickupSwap(source, target)
        ));
    }

    static RefillMove selectedRefillMove(final Inventory inventory, final int threshold) {
        int selected = inventory.getSelectedSlot();
        ItemStack selectedStack = inventory.getItem(selected);
        if (!shouldRefill(selectedStack, threshold)) {
            return null;
        }
        OptionalInt source = findLargestMatchingStack(inventory, selectedStack);
        return source.isEmpty() ? null : new RefillMove(selected, source.orElseThrow());
    }

    static boolean shouldRefill(final ItemStack stack, final int threshold) {
        return !stack.isEmpty()
                && stack.isStackable()
                && stack.getCount() <= Math.max(1, threshold)
                && stack.getCount() < stack.getMaxStackSize();
    }

    static OptionalInt findLargestMatchingStack(final Inventory inventory, final ItemStack target) {
        int bestSlot = -1;
        int bestCount = 0;
        for (int slot = Inventory.getSelectionSize(); slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty()
                    && stack.getCount() > bestCount
                    && ItemStack.isSameItemSameComponents(target, stack)) {
                bestSlot = slot;
                bestCount = stack.getCount();
            }
        }
        return bestSlot < 0 ? OptionalInt.empty() : OptionalInt.of(bestSlot);
    }

    record RefillMove(int targetSlot, int sourceSlot) {
    }
}
