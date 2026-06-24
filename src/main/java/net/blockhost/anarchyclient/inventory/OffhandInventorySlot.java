package net.blockhost.anarchyclient.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.OptionalInt;

public enum OffhandInventorySlot implements InventorySlotRef {
    INSTANCE;

    @Override
    public int menuSlot() {
        return InventoryMenu.SHIELD_SLOT;
    }

    @Override
    public OptionalInt inventorySlot() {
        return OptionalInt.of(Inventory.SLOT_OFFHAND);
    }
}
