package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoWeaponModuleTest {

    @Test
    void picksStrongestHotbarWeapon() {
        assertEquals(2, AutoWeaponModule.bestHotbarWeapon(TestInventories.withSlots(
                new ItemStack(Items.DIRT),
                new ItemStack(Items.STONE_SWORD),
                new ItemStack(Items.DIAMOND_SWORD))));
    }

    @Test
    void returnsNoSlotWithoutWeapons() {
        assertEquals(-1, AutoWeaponModule.bestHotbarWeapon(TestInventories.withSlots(new ItemStack(Items.DIRT))));
    }
}
