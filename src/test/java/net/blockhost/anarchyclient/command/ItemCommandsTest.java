package net.blockhost.anarchyclient.command;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MinecraftBootstrapExtension.class)
class ItemCommandsTest {

    @Test
    void givesToTheSelectedSlotWhenItIsEmpty() {
        Inventory inventory = TestInventories.withSlots(new ItemStack(Items.STONE));
        inventory.setSelectedSlot(3);

        assertEquals(3, ItemCommands.targetInventorySlot(inventory));
    }

    @Test
    void givesToTheFirstFreeSlotInsteadOfReplacingTheHeldItem() {
        Inventory inventory = TestInventories.withSlots(new ItemStack(Items.STONE), ItemStack.EMPTY, new ItemStack(Items.DIRT));
        inventory.setSelectedSlot(0);

        assertEquals(1, ItemCommands.targetInventorySlot(inventory));
    }

    @Test
    void mapsInventoryIndexesToCreativeMenuSlots() {
        assertEquals(36, ItemCommands.creativeMenuSlot(0));
        assertEquals(44, ItemCommands.creativeMenuSlot(8));
        assertEquals(9, ItemCommands.creativeMenuSlot(9));
        assertEquals(35, ItemCommands.creativeMenuSlot(35));
    }
}
