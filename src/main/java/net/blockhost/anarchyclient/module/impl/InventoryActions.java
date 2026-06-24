package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;
import java.util.function.Predicate;

final class InventoryActions {

    private InventoryActions() {
    }

    static OptionalInt findSlot(final Inventory inventory, final Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    static OptionalInt findHotbarSlot(final Inventory inventory, final Predicate<ItemStack> predicate) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    static boolean selectHotbarSlot(final LocalPlayer player, final int inventorySlot) {
        if (!Inventory.isHotbarSlot(inventorySlot)) {
            return false;
        }
        player.getInventory().setSelectedSlot(inventorySlot);
        return true;
    }

    static boolean moveToOffhand(final Minecraft client, final LocalPlayer player, final int inventorySlot) {
        return pickupSwap(client, player, InventorySlots.toInventoryMenuSlot(inventorySlot), InventoryMenu.SHIELD_SLOT);
    }

    static boolean equipArmor(final Minecraft client, final LocalPlayer player, final int inventorySlot, final EquipmentSlot armorSlot) {
        return pickupSwap(client, player, InventorySlots.toInventoryMenuSlot(inventorySlot), InventorySlots.armorMenuSlot(armorSlot));
    }

    static boolean pickupSwap(final Minecraft client, final LocalPlayer player, final int sourceMenuSlot, final int targetMenuSlot) {
        return InventoryAction.pickupSwap(sourceMenuSlot, targetMenuSlot).execute(client, player);
    }

    static int toInventoryMenuSlot(final int inventorySlot) {
        return InventorySlots.toInventoryMenuSlot(inventorySlot);
    }

    static int armorMenuSlot(final EquipmentSlot slot) {
        return InventorySlots.armorMenuSlot(slot);
    }

    static boolean canUseInventoryMenu(final Minecraft client, final LocalPlayer player) {
        return InventoryActionScheduler.canUseInventoryMenu(client, player);
    }
}
