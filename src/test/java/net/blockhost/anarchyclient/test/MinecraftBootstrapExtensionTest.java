package net.blockhost.anarchyclient.test;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MinecraftBootstrapExtension.class)
class MinecraftBootstrapExtensionTest {

    @Test
    void createsItemStacksWithVanillaComponents() {
        ItemStack stew = new ItemStack(Items.MUSHROOM_STEW);

        assertEquals(1, stew.getMaxStackSize());
        assertNotNull(stew.get(DataComponents.FOOD));
        assertEquals(64, new ItemStack(Items.DIRT).getMaxStackSize());
    }
}
