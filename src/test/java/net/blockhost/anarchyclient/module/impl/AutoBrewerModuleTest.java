package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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

    @Test
    void skipsBlazePowderWhenTheFuelSlotIsFull() {
        // Vanilla would put the extra blaze powder into the ingredient slot instead.
        ItemStack powder = new ItemStack(Items.BLAZE_POWDER);

        assertFalse(AutoBrewerModule.hasRoom(List.of(new ItemStack(Items.BLAZE_POWDER, 64)), powder));
        assertTrue(AutoBrewerModule.hasRoom(List.of(new ItemStack(Items.BLAZE_POWDER, 10)), powder));
        assertTrue(AutoBrewerModule.hasRoom(List.of(ItemStack.EMPTY), powder));
    }
}
