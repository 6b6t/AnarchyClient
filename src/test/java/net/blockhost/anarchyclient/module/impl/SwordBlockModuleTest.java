package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class SwordBlockModuleTest {

    @Test
    void blocksWithWeapons() {
        assertTrue(SwordBlockModule.canBlock(new ItemStack(Items.DIAMOND_SWORD)));
        assertTrue(SwordBlockModule.canBlock(new ItemStack(Items.NETHERITE_AXE)));
    }

    @Test
    void ignoresNonWeapons() {
        assertFalse(SwordBlockModule.canBlock(ItemStack.EMPTY));
        assertFalse(SwordBlockModule.canBlock(new ItemStack(Items.BOW)));
        assertFalse(SwordBlockModule.canBlock(new ItemStack(Items.ENDER_PEARL)));
        assertFalse(SwordBlockModule.canBlock(new ItemStack(Items.COBBLESTONE)));
    }

    @Test
    void leavesAMainHandShieldToVanilla() {
        assertFalse(SwordBlockModule.canBlock(new ItemStack(Items.SHIELD)));
    }
}
