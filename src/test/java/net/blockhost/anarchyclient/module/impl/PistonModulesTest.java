package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.test.MapBlockGetter;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class PistonModulesTest {

    private static final BlockPos FEET = new BlockPos(0, 64, 0);

    @Test
    void pistonRotationMakesVanillaPlaceThePistonFacingTheRequestedWay() {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            Rotation rotation = PistonPlacement.rotationFor(facing);
            Direction looking =
                    Direction.getApproximateNearest(Vec3.directionFromRotation(rotation.pitch(), rotation.yaw()));

            // Vanilla sets a piston's facing to the opposite of the nearest looking direction.
            assertEquals(facing, looking.getOpposite(), "facing " + facing);
        }
    }

    @Test
    void powerPositionsSkipThePistonFrontAndExcludedCells() {
        BlockPos piston = new BlockPos(5, 64, 5);
        BlockPos excluded = piston.above();

        List<BlockPos> positions = PistonPlacement.powerPositions(piston, Direction.EAST, List.of(excluded));

        assertFalse(positions.contains(piston.east()));
        assertFalse(positions.contains(excluded));
        assertEquals(4, positions.size());
        assertTrue(positions.contains(piston.west()));
    }

    @Test
    void pushNeedsAnOpenHeadCellAndAnOpenOrMatchingPistonCell() {
        MapBlockGetter level = holeAround(FEET);
        level.set(PistonPushModule.pistonPosition(FEET, Direction.NORTH), Blocks.STONE);

        List<Direction> facings = PistonPushModule.orderedFacings(level, FEET, Vec3.atCenterOf(FEET.south(3)));

        assertFalse(facings.contains(Direction.NORTH));
        assertEquals(3, facings.size());
        // A southward push needs its piston north of the target, the farthest spot from an eye in the south.
        assertEquals(Direction.SOUTH, facings.getLast());
    }

    @Test
    void pushIsImpossibleWhenTheHeadCellIsFilled() {
        MapBlockGetter level = holeAround(FEET).set(FEET.above(), Blocks.OBSIDIAN);

        assertTrue(PistonPushModule.orderedFacings(level, FEET, Vec3.ZERO).isEmpty());
    }

    @Test
    void pistonCrystalNeedsAnObsidianBaseBesideTheHead() {
        MapBlockGetter level = holeAround(FEET);
        level.set(FEET.east(), Blocks.STONE);

        List<Direction> directions = PistonCrystalModule.candidates(level, FEET, Vec3.atCenterOf(FEET.west(4)));

        assertFalse(directions.contains(Direction.EAST));
        assertEquals(Direction.WEST, directions.getFirst());
        PistonCrystalModule.Plan plan = PistonCrystalModule.Plan.of(FEET, Direction.WEST);
        assertEquals(FEET.above().west(), plan.crystal());
        assertEquals(FEET.above().west(2), plan.piston());
        assertEquals(Direction.EAST, plan.pistonFacing());
        // The crystal's wide hitbox leaves only the cells behind and below the piston for power.
        assertEquals(List.of(plan.piston().west(), plan.piston().below()), plan.power());
    }

    @Test
    void pistonCrystalNeedsAnOpenCrystalCellButNotRoomAboveIt() {
        MapBlockGetter level = holeAround(FEET);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.set(FEET.above(2).relative(direction), Blocks.STONE);
        }
        assertFalse(PistonCrystalModule.candidates(level, FEET, Vec3.ZERO).isEmpty());

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.set(FEET.above().relative(direction), Blocks.STONE);
        }
        assertTrue(PistonCrystalModule.candidates(level, FEET, Vec3.ZERO).isEmpty());
    }

    private static MapBlockGetter holeAround(final BlockPos feet) {
        MapBlockGetter level = new MapBlockGetter().set(feet.below(), Blocks.BEDROCK);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            level.set(feet.relative(direction), Blocks.OBSIDIAN);
        }
        return level;
    }
}
