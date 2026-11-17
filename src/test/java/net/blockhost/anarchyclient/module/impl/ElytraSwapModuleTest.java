package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class ElytraSwapModuleTest {

    @Test
    void findsFirstUsableElytra() {
        ItemStack broken = new ItemStack(Items.ELYTRA);
        broken.setDamageValue(broken.getMaxDamage() - 1);

        assertEquals(2, ElytraSwapModule.findElytra(TestInventories.withSlots(
                new ItemStack(Items.DIRT), broken, new ItemStack(Items.ELYTRA))));
    }

    @Test
    void returnsNoSlotWithoutUsableElytra() {
        assertEquals(-1, ElytraSwapModule.findElytra(TestInventories.withSlots(new ItemStack(Items.DIAMOND_CHESTPLATE))));
    }

    @Test
    void equipsOnlyWhileFallingFreely() {
        assertTrue(ElytraSwapModule.isFalling(false, 3.0, false, false));
        assertFalse(ElytraSwapModule.isFalling(true, 3.0, false, false));
        assertFalse(ElytraSwapModule.isFalling(false, 1.0, false, false));
        assertFalse(ElytraSwapModule.isFalling(false, 3.0, true, false));
        assertFalse(ElytraSwapModule.isFalling(false, 3.0, false, true));
    }
}
