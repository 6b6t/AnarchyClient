package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class ChestStealerModuleTest {

    @Test
    void skipsItemsThatDoNotFitInAFullInventory() {
        List<ItemStack> stacks = List.of(
                new ItemStack(Items.DIAMOND),
                new ItemStack(Items.STONE, 64));

        assertEquals(-1, ChestStealerModule.firstStealableSlot(stacks, 1, false, Set.of()));
        assertTrue(ChestStealerModule.hasStealable(stacks, 1, false, Set.of()));
    }

    @Test
    void takesItemsThatStackOntoAPartialInventorySlot() {
        List<ItemStack> stacks = List.of(
                new ItemStack(Items.DIAMOND),
                new ItemStack(Items.DIAMOND, 10));

        assertEquals(0, ChestStealerModule.firstStealableSlot(stacks, 1, false, Set.of()));
    }
}
