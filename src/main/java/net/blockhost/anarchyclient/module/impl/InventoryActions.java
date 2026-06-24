package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
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
        return pickupSwap(client, player, toInventoryMenuSlot(inventorySlot), InventoryMenu.SHIELD_SLOT);
    }

    static boolean equipArmor(final Minecraft client, final LocalPlayer player, final int inventorySlot, final EquipmentSlot armorSlot) {
        return pickupSwap(client, player, toInventoryMenuSlot(inventorySlot), armorMenuSlot(armorSlot));
    }

    static boolean pickupSwap(final Minecraft client, final LocalPlayer player, final int sourceMenuSlot, final int targetMenuSlot) {
        if (client.gameMode == null || sourceMenuSlot < 0 || targetMenuSlot < 0) {
            return false;
        }
        client.gameMode.handleContainerInput(InventoryMenu.CONTAINER_ID, sourceMenuSlot, 0, ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(InventoryMenu.CONTAINER_ID, targetMenuSlot, 0, ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(InventoryMenu.CONTAINER_ID, sourceMenuSlot, 0, ContainerInput.PICKUP, player);
        return true;
    }

    static int toInventoryMenuSlot(final int inventorySlot) {
        if (Inventory.isHotbarSlot(inventorySlot)) {
            return InventoryMenu.USE_ROW_SLOT_START + inventorySlot;
        }
        if (inventorySlot >= Inventory.getSelectionSize() && inventorySlot < Inventory.INVENTORY_SIZE) {
            return InventoryMenu.INV_SLOT_START + inventorySlot - Inventory.getSelectionSize();
        }
        return -1;
    }

    static int armorMenuSlot(final EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> InventoryMenu.ARMOR_SLOT_START;
            case CHEST -> InventoryMenu.ARMOR_SLOT_START + 1;
            case LEGS -> InventoryMenu.ARMOR_SLOT_START + 2;
            case FEET -> InventoryMenu.ARMOR_SLOT_START + 3;
            default -> -1;
        };
    }

    static boolean canUseInventoryMenu(final Minecraft client, final LocalPlayer player) {
        return client.gameMode != null
                && client.gui.screen() == null
                && player.containerMenu != null
                && player.inventoryMenu != null
                && player.containerMenu.containerId == InventoryMenu.CONTAINER_ID;
    }
}
