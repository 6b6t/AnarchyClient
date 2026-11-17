package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoSmelterModuleTest {

    @Test
    void loadsOnlyIntoAnEmptyOrMatchingSlotWithRoom() {
        assertTrue(AutoSmelterModule.canStackInto(ItemStack.EMPTY, new ItemStack(Items.COAL)));
        assertTrue(AutoSmelterModule.canStackInto(new ItemStack(Items.COAL, 10), new ItemStack(Items.COAL)));
        assertFalse(AutoSmelterModule.canStackInto(new ItemStack(Items.COAL, 64), new ItemStack(Items.COAL)));
        assertFalse(AutoSmelterModule.canStackInto(new ItemStack(Items.CHARCOAL, 10), new ItemStack(Items.COAL)));
    }

    @Test
    void usesCoalAndCharcoalAsFuel() {
        assertTrue(AutoSmelterModule.isFuel(new ItemStack(Items.COAL)));
        assertTrue(AutoSmelterModule.isFuel(new ItemStack(Items.CHARCOAL)));
        assertFalse(AutoSmelterModule.isFuel(new ItemStack(Items.COAL_BLOCK)));
    }
}
