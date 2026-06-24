package net.blockhost.anarchyclient.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.OptionalInt;

public record HotbarInventorySlot(int index) implements InventorySlotRef {

    public HotbarInventorySlot {
        if (!Inventory.isHotbarSlot(index)) {
            throw new IllegalArgumentException("Invalid hotbar slot: " + index);
        }
    }

    @Override
    public int menuSlot() {
        return InventoryMenu.USE_ROW_SLOT_START + this.index;
    }

    @Override
    public OptionalInt inventorySlot() {
        return OptionalInt.of(this.index);
    }
}
