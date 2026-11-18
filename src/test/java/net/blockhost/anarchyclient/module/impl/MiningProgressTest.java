package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class MiningProgressTest {

    @Test
    void efficiencyAddsLevelSquaredPlusOneToFastTools() {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);

        assertEquals(8.0F, MiningProgress.toolSpeed(pickaxe, Blocks.OBSIDIAN.defaultBlockState(), 0));
        assertEquals(34.0F, MiningProgress.toolSpeed(pickaxe, Blocks.OBSIDIAN.defaultBlockState(), 5));
    }

    @Test
    void efficiencyDoesNotHelpTheWrongTool() {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);

        assertEquals(1.0F, MiningProgress.toolSpeed(pickaxe, Blocks.OAK_LOG.defaultBlockState(), 5));
    }

    @Test
    void obsidianNeedsAPickaxeOfDiamondOrBetter() {
        assertTrue(MiningProgress.canHarvest(new ItemStack(Items.DIAMOND_PICKAXE), Blocks.OBSIDIAN.defaultBlockState()));
        assertFalse(MiningProgress.canHarvest(new ItemStack(Items.IRON_PICKAXE), Blocks.OBSIDIAN.defaultBlockState()));
        assertTrue(MiningProgress.canHarvest(ItemStack.EMPTY, Blocks.DIRT.defaultBlockState()));
    }

    @Test
    void serverAcceptsAFinishOnceSeventyPercentIsReached() {
        // Diamond pickaxe with Efficiency V on obsidian: 34 / 50 / 30 per tick.
        float perTick = 34.0F / 50.0F / 30.0F;

        assertFalse(MiningProgress.serverAccepts(perTick, 29));
        assertTrue(MiningProgress.serverAccepts(perTick, 30));
    }
}
