package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class TargetPlacementPlannerTest {

    @Test
    void acceptsOnlyObsidianAndBedrockBases() {
        assertTrue(TargetPlacementPlanner.isCrystalBase(Blocks.OBSIDIAN.defaultBlockState()));
        assertTrue(TargetPlacementPlanner.isCrystalBase(Blocks.BEDROCK.defaultBlockState()));
        assertFalse(TargetPlacementPlanner.isCrystalBase(Blocks.CRYING_OBSIDIAN.defaultBlockState()));
    }

    @Test
    void checksTheSameTwoBlockBoxAsEndCrystalItem() {
        assertEquals(new AABB(4.0, 65.0, -2.0, 5.0, 67.0, -1.0), TargetPlacementPlanner.crystalSpace(new BlockPos(4, 65, -2)));
    }
}
