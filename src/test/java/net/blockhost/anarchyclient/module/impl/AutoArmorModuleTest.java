package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoArmorModuleTest {

    @Test
    void upgradesToBetterArmorForTheSlot() {
        assertEquals(1, AutoArmorModule.bestArmorSlot(TestInventories.withSlots(
                        new ItemStack(Items.DIAMOND_HELMET),
                        new ItemStack(Items.DIAMOND_CHESTPLATE)),
                EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE),
                EquipmentScorer.ProtectionPreference.PROTECTION, Set.of(), false));
    }

    @Test
    void keepsCurrentArmorWhenNothingIsBetter() {
        assertEquals(-1, AutoArmorModule.bestArmorSlot(TestInventories.withSlots(new ItemStack(Items.IRON_CHESTPLATE)),
                EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE),
                EquipmentScorer.ProtectionPreference.PROTECTION, Set.of(), false));
    }
}
