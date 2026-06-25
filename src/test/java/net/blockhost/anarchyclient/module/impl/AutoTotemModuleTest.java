package net.blockhost.anarchyclient.module.impl;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoTotemModuleTest {

    @Test
    void mapsHotbarSlotsToUseRowSlots() {
        assertEquals(InventoryMenu.USE_ROW_SLOT_START, AutoTotemModule.toInventoryMenuSlot(0));
        assertEquals(InventoryMenu.USE_ROW_SLOT_START + Inventory.getSelectionSize() - 1, AutoTotemModule.toInventoryMenuSlot(Inventory.getSelectionSize() - 1));
    }

    @Test
    void mapsMainInventorySlotsToInventoryMenuSlots() {
        assertEquals(InventoryMenu.INV_SLOT_START, AutoTotemModule.toInventoryMenuSlot(Inventory.getSelectionSize()));
        assertEquals(InventoryMenu.INV_SLOT_START + 26, AutoTotemModule.toInventoryMenuSlot(Inventory.INVENTORY_SIZE - 1));
    }

    @Test
    void ignoresNonStorageSlots() {
        assertEquals(-1, AutoTotemModule.toInventoryMenuSlot(-1));
        assertEquals(-1, AutoTotemModule.toInventoryMenuSlot(Inventory.INVENTORY_SIZE));
    }

    @Test
    void estimatesExplosionDamageByDistance() {
        assertEquals(20.0, AutoTotemModule.estimateExplosionDamage(0.0, 6.0, 20.0));
        assertEquals(7.5, AutoTotemModule.estimateExplosionDamage(9.0, 6.0, 20.0));
        assertEquals(0.0, AutoTotemModule.estimateExplosionDamage(36.0, 6.0, 20.0));
    }

    @Test
    void calculatesDamageUntilThreshold() {
        assertEquals(8.0, AutoTotemModule.damageUntilThreshold(20.0, 12.0));
        assertEquals(0.0, AutoTotemModule.damageUntilThreshold(8.0, 12.0));
    }
}
