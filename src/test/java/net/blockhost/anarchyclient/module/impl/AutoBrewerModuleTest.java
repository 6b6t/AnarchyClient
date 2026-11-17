package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoBrewerModuleTest {

    @Test
    void treatsWaterBottlesAsBrewingInput() {
        assertTrue(AutoBrewerModule.isWaterBottle(PotionContents.createItemStack(Items.POTION, Potions.WATER)));
        assertTrue(AutoBrewerModule.isWaterBottle(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER)));
    }

    @Test
    void treatsBrewedPotionsAsFinished() {
        assertFalse(AutoBrewerModule.isWaterBottle(PotionContents.createItemStack(Items.POTION, Potions.AWKWARD)));
        assertFalse(AutoBrewerModule.isWaterBottle(PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS)));
        assertFalse(AutoBrewerModule.isWaterBottle(new ItemStack(Items.GLASS_BOTTLE)));
    }
}
