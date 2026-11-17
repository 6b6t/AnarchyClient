package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoToolModuleTest {

    @Test
    void picksFastestCorrectTool() {
        assertEquals(2, AutoToolModule.bestToolSlot(TestInventories.withSlots(
                new ItemStack(Items.DIAMOND_SHOVEL),
                new ItemStack(Items.WOODEN_PICKAXE),
                new ItemStack(Items.DIAMOND_PICKAXE)), Blocks.STONE.defaultBlockState(), false));
    }

    @Test
    void returnsNoSlotWhenNothingMinesFaster() {
        assertEquals(-1, AutoToolModule.bestToolSlot(TestInventories.withSlots(new ItemStack(Items.DIRT)),
                Blocks.STONE.defaultBlockState(), false));
    }

    @Test
    void skipsToolsAboutToBreakUnlessDurabilityIsIgnored() {
        ItemStack worn = new ItemStack(Items.DIAMOND_PICKAXE);
        worn.setDamageValue(worn.getMaxDamage() - 1);

        assertEquals(-1, AutoToolModule.bestToolSlot(TestInventories.withSlots(worn), Blocks.STONE.defaultBlockState(), false));
        assertEquals(0, AutoToolModule.bestToolSlot(TestInventories.withSlots(worn), Blocks.STONE.defaultBlockState(), true));
    }
}
