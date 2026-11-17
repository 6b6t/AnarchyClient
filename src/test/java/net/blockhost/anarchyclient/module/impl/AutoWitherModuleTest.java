package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.impl.AutoWitherModule.WitherMaterial;
import net.blockhost.anarchyclient.module.impl.AutoWitherModule.WitherStep;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoWitherModuleTest {

    @Test
    void placesAllSoulBlocksBeforeAnySkull() {
        // The wither spawns when the last skull lands, so every soul block must already be in place.
        List<WitherMaterial> materials = AutoWitherModule.planSteps(BlockPos.ZERO, Direction.Axis.X).stream()
                .map(WitherStep::material)
                .toList();

        assertEquals(List.of(
                WitherMaterial.SOUL, WitherMaterial.SOUL, WitherMaterial.SOUL, WitherMaterial.SOUL,
                WitherMaterial.SKULL, WitherMaterial.SKULL, WitherMaterial.SKULL
        ), materials);
    }

    @Test
    void buildsArmsAlongTheZAxis() {
        BlockPos foot = new BlockPos(10, 64, 10);

        assertEquals(List.of(
                foot,
                foot.above(),
                foot.above().north(),
                foot.above().south(),
                foot.above(2).north(),
                foot.above(2),
                foot.above(2).south()
        ), AutoWitherModule.planPositions(foot, Direction.Axis.Z));
    }
}
