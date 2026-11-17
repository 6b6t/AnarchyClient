package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MinecraftBootstrapExtension.class)
class AnchorAuraModuleTest {

    @Test
    void keepsTheSelectedSlotWhenItHoldsNoGlowstone() {
        Inventory inventory = TestInventories.withSlots(new ItemStack(Items.GLOWSTONE), new ItemStack(Items.DIAMOND_SWORD));
        inventory.setSelectedSlot(1);

        assertEquals(OptionalInt.of(1), AnchorAuraModule.detonationSlot(inventory));
    }

    @Test
    void switchesAwayFromGlowstoneSoTheClickDetonates() {
        Inventory inventory = TestInventories.withSlots(new ItemStack(Items.GLOWSTONE), new ItemStack(Items.GLOWSTONE));
        inventory.setSelectedSlot(0);

        assertEquals(OptionalInt.of(2), AnchorAuraModule.detonationSlot(inventory));
    }

    @Test
    void findsNoSlotWhenTheWholeHotbarIsGlowstone() {
        ItemStack[] stacks = new ItemStack[Inventory.getSelectionSize()];
        for (int slot = 0; slot < stacks.length; slot++) {
            stacks[slot] = new ItemStack(Items.GLOWSTONE);
        }

        assertEquals(OptionalInt.empty(), AnchorAuraModule.detonationSlot(TestInventories.withSlots(stacks)));
    }
}
