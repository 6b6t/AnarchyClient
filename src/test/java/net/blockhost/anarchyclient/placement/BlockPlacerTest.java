package net.blockhost.anarchyclient.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockPlacerTest {

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
