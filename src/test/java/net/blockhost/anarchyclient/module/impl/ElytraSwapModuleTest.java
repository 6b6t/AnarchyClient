package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
