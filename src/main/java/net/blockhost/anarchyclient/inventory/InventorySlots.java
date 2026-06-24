package net.blockhost.anarchyclient.inventory;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.Optional;

public final class InventorySlots {

    private InventorySlots() {
    }

    public static int toInventoryMenuSlot(final int inventorySlot) {
        if (Inventory.isHotbarSlot(inventorySlot)) {
            return new HotbarInventorySlot(inventorySlot).menuSlot();
        }
        if (inventorySlot >= Inventory.getSelectionSize() && inventorySlot < Inventory.INVENTORY_SIZE) {
            return new MainInventorySlot(inventorySlot - Inventory.getSelectionSize()).menuSlot();
        }
        return -1;
    }

    public static Optional<InventorySlotRef> storageSlot(final int inventorySlot) {
        if (Inventory.isHotbarSlot(inventorySlot)) {
            return Optional.of(new HotbarInventorySlot(inventorySlot));
        }
        if (inventorySlot >= Inventory.getSelectionSize() && inventorySlot < Inventory.INVENTORY_SIZE) {
            return Optional.of(new MainInventorySlot(inventorySlot - Inventory.getSelectionSize()));
        }
        return Optional.empty();
    }

    public static ArmorInventorySlot armorSlot(final EquipmentSlot slot) {
        return new ArmorInventorySlot(slot);
    }

    public static OffhandInventorySlot offhandSlot() {
        return OffhandInventorySlot.INSTANCE;
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
