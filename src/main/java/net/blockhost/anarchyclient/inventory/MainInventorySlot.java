package net.blockhost.anarchyclient.inventory;

import java.util.OptionalInt;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

public record MainInventorySlot(int index) implements InventorySlotRef {

    public MainInventorySlot {
        if (index < 0 || index >= Inventory.INVENTORY_SIZE - Inventory.getSelectionSize()) {
            throw new IllegalArgumentException("Invalid main inventory slot: " + index);
        }
    }

    @Override
    public int menuSlot() {
        return InventoryMenu.INV_SLOT_START + this.index;
    }

    @Override
    public OptionalInt inventorySlot() {
        return OptionalInt.of(Inventory.getSelectionSize() + this.index);
    }
}
