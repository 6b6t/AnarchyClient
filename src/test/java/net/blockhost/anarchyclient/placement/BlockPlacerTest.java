package net.blockhost.anarchyclient.placement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class BlockPlacerTest {

    @Test
    void detectsNeighboursThatReactToRightClicks() {
        assertTrue(BlockPlacer.hasUseInteraction(Blocks.CRAFTING_TABLE.defaultBlockState()));
        assertTrue(BlockPlacer.hasUseInteraction(Blocks.RESPAWN_ANCHOR.defaultBlockState()));
        assertTrue(BlockPlacer.hasUseInteraction(Blocks.CHEST.defaultBlockState()));
        assertTrue(BlockPlacer.hasUseInteraction(Blocks.OAK_DOOR.defaultBlockState()));
        assertFalse(BlockPlacer.hasUseInteraction(Blocks.OBSIDIAN.defaultBlockState()));
        assertFalse(BlockPlacer.hasUseInteraction(Blocks.STONE.defaultBlockState()));
        assertFalse(BlockPlacer.hasUseInteraction(Blocks.NETHERRACK.defaultBlockState()));
    }

    @Test
    void placementOptionsExposeStrictAndNonFullModes() {
        assertTrue(BlockPlacer.PlacementOptions.DEFAULT.requireFullBlock());
        assertFalse(BlockPlacer.PlacementOptions.DEFAULT.allowFallingBlocks());
        assertFalse(BlockPlacer.PlacementOptions.NON_FULL.requireFullBlock());
        assertFalse(BlockPlacer.PlacementOptions.NON_FULL.allowFallingBlocks());
        assertFalse(BlockPlacer.PlacementOptions.ANY_BLOCK_ITEM.requireFullBlock());
        assertTrue(BlockPlacer.PlacementOptions.ANY_BLOCK_ITEM.allowFallingBlocks());
    }

    @Test
    void placementRequestsDefaultToStrictOptions() {
        BlockPos target = new BlockPos(1, 2, 3);
        BlockPlacer.PlacementRequest request = new BlockPlacer.PlacementRequest(target, ItemStack::isEmpty);

        assertEquals(target, request.target());
        assertSame(BlockPlacer.PlacementOptions.DEFAULT, request.options());
    }

    @Test
    void placementLimitsNeverDropBelowOne() {
        assertEquals(1, BlockPlacer.placementsPerTick(0));
        assertEquals(1, BlockPlacer.placementsPerTick(-5));
        assertEquals(4, BlockPlacer.placementsPerTick(4));
    }
}
