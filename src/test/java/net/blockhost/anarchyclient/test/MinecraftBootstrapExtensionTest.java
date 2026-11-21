package net.blockhost.anarchyclient.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class MinecraftBootstrapExtensionTest {

    @Test
    void createsItemStacksWithVanillaComponents() {
        ItemStack stew = new ItemStack(Items.MUSHROOM_STEW);

        assertEquals(1, stew.getMaxStackSize());
        assertNotNull(stew.get(DataComponents.FOOD));
        assertEquals(64, new ItemStack(Items.DIRT).getMaxStackSize());
    }

    @Test
    void bindsVanillaTags() {
        assertTrue(Blocks.STONE.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE));
        assertFalse(Blocks.DIRT.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE));
    }
}
