package net.blockhost.anarchyclient.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class ToolSelectorTest {

    @Test
    void ranksHarvestingToolsAboveFasterWrongTools() {
        BlockState stone = Blocks.STONE.defaultBlockState();

        double woodenPickaxe = ToolSelector.score(new ItemStack(Items.WOODEN_PICKAXE), stone);
        double goldenShovel = ToolSelector.score(new ItemStack(Items.GOLDEN_SHOVEL), stone);
        double diamondPickaxe = ToolSelector.score(new ItemStack(Items.DIAMOND_PICKAXE), stone);

        assertTrue(woodenPickaxe > goldenShovel);
        assertTrue(diamondPickaxe > woodenPickaxe);
    }

    @Test
    void ranksByDestroySpeedWhenEveryItemHarvests() {
        BlockState dirt = Blocks.DIRT.defaultBlockState();

        assertTrue(ToolSelector.score(new ItemStack(Items.IRON_SHOVEL), dirt)
                > ToolSelector.score(new ItemStack(Items.DIAMOND_SWORD), dirt));
        assertEquals(ToolSelector.score(ItemStack.EMPTY, dirt), ToolSelector.score(new ItemStack(Items.STICK), dirt));
    }
}
