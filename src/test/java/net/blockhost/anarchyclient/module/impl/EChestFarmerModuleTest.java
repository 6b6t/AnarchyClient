package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class EChestFarmerModuleTest {

    @Test
    void minesWithAPickaxeOnly() {
        assertTrue(EChestFarmerModule.isFarmingPickaxe(new ItemStack(Items.DIAMOND_PICKAXE)));
        assertFalse(EChestFarmerModule.isFarmingPickaxe(new ItemStack(Items.DIAMOND_SHOVEL)));
        assertFalse(EChestFarmerModule.isFarmingPickaxe(ItemStack.EMPTY));
    }
}
