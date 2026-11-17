package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoReplenishModuleTest {

    @Test
    void replenishesOnlyLowStackableStacks() {
        assertTrue(AutoReplenishModule.shouldReplenish(new ItemStack(Items.COBBLESTONE, 4), 8));
        assertFalse(AutoReplenishModule.shouldReplenish(new ItemStack(Items.COBBLESTONE, 32), 8));
        assertFalse(AutoReplenishModule.shouldReplenish(new ItemStack(Items.DIAMOND_SWORD), 8));
        assertFalse(AutoReplenishModule.shouldReplenish(ItemStack.EMPTY, 8));
    }

    @Test
    void refillsLowHotbarStackFromMainInventory() {
        ItemStack[] slots = new ItemStack[Inventory.getSelectionSize() + 1];
        Arrays.fill(slots, ItemStack.EMPTY);
        slots[2] = new ItemStack(Items.COBBLESTONE, 3);
        slots[Inventory.getSelectionSize()] = new ItemStack(Items.COBBLESTONE, 64);

        AutoReplenishModule.ReplenishMove move = AutoReplenishModule.findReplenishMove(TestInventories.withSlots(slots), 8);

        assertEquals(new AutoReplenishModule.ReplenishMove(2, Inventory.getSelectionSize()), move);
    }

    @Test
    void skipsStacksWithoutAMatchingReplacement() {
        Inventory inventory = TestInventories.withSlots(new ItemStack(Items.COBBLESTONE, 3));

        assertNull(AutoReplenishModule.findReplenishMove(inventory, 8));
    }
}
