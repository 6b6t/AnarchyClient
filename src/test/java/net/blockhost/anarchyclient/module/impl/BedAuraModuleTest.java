package net.blockhost.anarchyclient.module.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BedAuraModuleTest {

    private static final Vec3 EYE = new Vec3(0.5, 1.62, 0.5);

    @Test
    void bedPointsAwayFromThePlayer() {
        assertEquals(Optional.of(Direction.NORTH), BedAuraModule.placementFacing(EYE, new BlockPos(0, 0, -2)));
        assertEquals(Optional.of(Direction.SOUTH), BedAuraModule.placementFacing(EYE, new BlockPos(0, 0, 2)));
        assertEquals(Optional.of(Direction.EAST), BedAuraModule.placementFacing(EYE, new BlockPos(2, 0, 1)));
        assertEquals(Optional.of(Direction.WEST), BedAuraModule.placementFacing(EYE, new BlockPos(-2, 0, 0)));
    }

    @Test
    void skipsDiagonalPlacementsWithAnUnreliableFacing() {
        assertEquals(Optional.empty(), BedAuraModule.placementFacing(EYE, new BlockPos(2, 0, 2)));
    }

    @Test
    void skipsTheBlockDirectlyBelowThePlayer() {
        assertEquals(Optional.empty(), BedAuraModule.placementFacing(EYE, new BlockPos(0, -1, 0)));
    }
}
