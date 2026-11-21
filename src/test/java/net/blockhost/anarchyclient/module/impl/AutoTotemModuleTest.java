package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.junit.jupiter.api.Test;

class AutoTotemModuleTest {

    @Test
    void mapsHotbarSlotsToUseRowSlots() {
        assertEquals(InventoryMenu.USE_ROW_SLOT_START, AutoTotemModule.toInventoryMenuSlot(0));
        assertEquals(
                InventoryMenu.USE_ROW_SLOT_START + Inventory.getSelectionSize() - 1,
                AutoTotemModule.toInventoryMenuSlot(Inventory.getSelectionSize() - 1));
    }

    @Test
    void mapsMainInventorySlotsToInventoryMenuSlots() {
        assertEquals(InventoryMenu.INV_SLOT_START, AutoTotemModule.toInventoryMenuSlot(Inventory.getSelectionSize()));
        assertEquals(
                InventoryMenu.INV_SLOT_START + 26, AutoTotemModule.toInventoryMenuSlot(Inventory.INVENTORY_SIZE - 1));
    }

    @Test
    void ignoresNonStorageSlots() {
        assertEquals(-1, AutoTotemModule.toInventoryMenuSlot(-1));
        assertEquals(-1, AutoTotemModule.toInventoryMenuSlot(Inventory.INVENTORY_SIZE));
    }

    @Test
    void calculatesDamageUntilThreshold() {
        assertEquals(8.0, AutoTotemModule.damageUntilThreshold(20.0, 12.0));
        assertEquals(0.0, AutoTotemModule.damageUntilThreshold(8.0, 12.0));
    }
}
