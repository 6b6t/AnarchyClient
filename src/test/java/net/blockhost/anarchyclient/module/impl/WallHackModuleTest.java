package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MinecraftBootstrapExtension.class)
class WallHackModuleTest {

    private static final BlockPos ORE = new BlockPos(0, -40, 0);
    private static final BlockPos MINED = new BlockPos(3, -40, 0);
    private static final BlockPos DESELECTED = new BlockPos(6, -40, 0);

    @Test
    void dropsHighlightsWhoseBlockWasMinedOrIsNoLongerSelected() {
        Map<BlockPos, Block> world = Map.of(
                ORE, Blocks.DIAMOND_ORE,
                MINED, Blocks.AIR,
                DESELECTED, Blocks.EMERALD_ORE
        );

        List<BlockPos> stale = WallHackModule.stalePositions(List.of(ORE, MINED, DESELECTED),
                Set.of(Blocks.DIAMOND_ORE), world::get);

        assertEquals(List.of(MINED, DESELECTED), stale);
    }

    @Test
    void keepsHighlightsThatStillMatch() {
        List<BlockPos> stale = WallHackModule.stalePositions(List.of(ORE), Set.of(Blocks.DIAMOND_ORE),
                pos -> Blocks.DIAMOND_ORE);

        assertEquals(List.of(), stale);
    }
}
