package net.blockhost.anarchyclient.module.impl;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryActionsTest {

    @Test
    void mapsHotbarSlotsToUseRowSlots() {
        assertEquals(InventoryMenu.USE_ROW_SLOT_START, InventoryActions.toInventoryMenuSlot(0));
        assertEquals(InventoryMenu.USE_ROW_SLOT_START + Inventory.getSelectionSize() - 1,
                InventoryActions.toInventoryMenuSlot(Inventory.getSelectionSize() - 1));
    }

    @Test
    void mapsMainInventorySlotsToInventoryMenuSlots() {
        assertEquals(InventoryMenu.INV_SLOT_START, InventoryActions.toInventoryMenuSlot(Inventory.getSelectionSize()));
        assertEquals(InventoryMenu.INV_SLOT_START + 26, InventoryActions.toInventoryMenuSlot(Inventory.INVENTORY_SIZE - 1));
    }

    @Test
    void mapsArmorSlotsToMenuSlots() {
        assertEquals(InventoryMenu.ARMOR_SLOT_START, InventoryActions.armorMenuSlot(EquipmentSlot.HEAD));
        assertEquals(InventoryMenu.ARMOR_SLOT_START + 1, InventoryActions.armorMenuSlot(EquipmentSlot.CHEST));
        assertEquals(InventoryMenu.ARMOR_SLOT_START + 2, InventoryActions.armorMenuSlot(EquipmentSlot.LEGS));
        assertEquals(InventoryMenu.ARMOR_SLOT_START + 3, InventoryActions.armorMenuSlot(EquipmentSlot.FEET));
        assertEquals(-1, InventoryActions.armorMenuSlot(EquipmentSlot.MAINHAND));
    }
}
