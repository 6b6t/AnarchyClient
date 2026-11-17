package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class FastPlaceModuleTest {

    @Test
    void blocksOnlyAcceleratesBlocksAndEnabledExceptions() {
        assertTrue(FastPlaceModule.shouldAccelerate(new ItemStack(Items.STONE), true, false, false));
        assertFalse(FastPlaceModule.shouldAccelerate(new ItemStack(Items.EXPERIENCE_BOTTLE), true, false, false));
        assertTrue(FastPlaceModule.shouldAccelerate(new ItemStack(Items.EXPERIENCE_BOTTLE), true, true, false));
        assertFalse(FastPlaceModule.shouldAccelerate(new ItemStack(Items.END_CRYSTAL), true, true, false));
        assertTrue(FastPlaceModule.shouldAccelerate(new ItemStack(Items.END_CRYSTAL), true, false, true));
        assertFalse(FastPlaceModule.shouldAccelerate(new ItemStack(Items.SNOWBALL), true, true, true));
    }

    @Test
    void disablingBlocksOnlyAcceleratesEveryItem() {
        assertTrue(FastPlaceModule.shouldAccelerate(new ItemStack(Items.SNOWBALL), false, false, false));
        assertFalse(FastPlaceModule.shouldAccelerate(ItemStack.EMPTY, false, true, true));
    }
}
