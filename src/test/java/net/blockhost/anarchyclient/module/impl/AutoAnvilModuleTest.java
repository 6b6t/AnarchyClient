package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class AutoAnvilModuleTest {

    private static final BlockPos BASE = new BlockPos(10, 64, 10);

    @Test
    void keepsTheDropColumnOpen() {
        for (int dropHeight = 2; dropHeight <= 6; dropHeight++) {
            List<BlockPos> supports = AutoAnvilModule.supportPositions(BASE, dropHeight, true, Direction.NORTH);

            for (int y = 1; y < dropHeight; y++) {
                assertFalse(supports.contains(BASE.above(y)), "block under the anvil at height " + y);
            }
        }
    }

    @Test
    void supportsTheAnvilFromTheChosenSide() {
        List<BlockPos> supports = AutoAnvilModule.supportPositions(BASE, 3, false, Direction.EAST);

        assertEquals(List.of(BASE.above(3).east()), supports);
    }

    @Test
    void faceTrapSurroundsTheHead() {
        List<BlockPos> supports = AutoAnvilModule.supportPositions(BASE, 3, true, Direction.NORTH);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            assertTrue(supports.contains(BASE.above().relative(direction)));
        }
        assertEquals(5, supports.size());
    }

    @Test
    void picksTheSideFacingThePlacer() {
        assertEquals(Direction.SOUTH, AutoAnvilModule.supportSide(BASE, new Vec3(10.5, 64.0, 14.0)));
        assertEquals(Direction.WEST, AutoAnvilModule.supportSide(BASE, new Vec3(7.0, 64.0, 10.4)));
        assertEquals(Direction.NORTH, AutoAnvilModule.supportSide(BASE, new Vec3(10.5, 64.0, 10.5)));
    }
}
