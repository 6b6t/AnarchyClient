package net.blockhost.anarchyclient.inventory;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

public final class InventorySlots {

    private InventorySlots() {
    }

    public static int toInventoryMenuSlot(final int inventorySlot) {
        if (Inventory.isHotbarSlot(inventorySlot)) {
            return InventoryMenu.USE_ROW_SLOT_START + inventorySlot;
        }
        if (inventorySlot >= Inventory.getSelectionSize() && inventorySlot < Inventory.INVENTORY_SIZE) {
            return InventoryMenu.INV_SLOT_START + inventorySlot - Inventory.getSelectionSize();
        }
        return -1;
    }

    public static int armorMenuSlot(final EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> InventoryMenu.ARMOR_SLOT_START;
            case CHEST -> InventoryMenu.ARMOR_SLOT_START + 1;
            case LEGS -> InventoryMenu.ARMOR_SLOT_START + 2;
            case FEET -> InventoryMenu.ARMOR_SLOT_START + 3;
            default -> -1;
        };
    }
}
