package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.blockhost.anarchyclient.test.TestInventories;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoArmorModuleTest {

    @Test
    void upgradesToBetterArmorForTheSlot() {
        assertEquals(
                1,
                AutoArmorModule.bestArmorSlot(
                        TestInventories.withSlots(
                                new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE)),
                        EquipmentSlot.CHEST,
                        new ItemStack(Items.IRON_CHESTPLATE),
                        EquipmentScorer.ProtectionPreference.PROTECTION,
                        Set.of(),
                        false));
    }

    @Test
    void keepsCurrentArmorWhenNothingIsBetter() {
        assertEquals(
                -1,
                AutoArmorModule.bestArmorSlot(
                        TestInventories.withSlots(new ItemStack(Items.IRON_CHESTPLATE)),
                        EquipmentSlot.CHEST,
                        new ItemStack(Items.NETHERITE_CHESTPLATE),
                        EquipmentScorer.ProtectionPreference.PROTECTION,
                        Set.of(),
                        false));
    }

    @Test
    void ignoresArmorHeldInTheOffhand() {
        // The offhand has no storage slot to swap from, so picking it would block the upgrade.
        Inventory inventory = TestInventories.withSlots(new ItemStack(Items.IRON_HELMET));
        inventory.setItem(Inventory.SLOT_OFFHAND, new ItemStack(Items.NETHERITE_HELMET));

        assertEquals(
                0,
                AutoArmorModule.bestArmorSlot(
                        inventory,
                        EquipmentSlot.HEAD,
                        ItemStack.EMPTY,
                        EquipmentScorer.ProtectionPreference.PROTECTION,
                        Set.of(),
                        false));
    }
}
