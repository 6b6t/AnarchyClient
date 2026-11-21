package net.blockhost.anarchyclient.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class HoleManager {

    private HoleManager() {}

    public static List<BlockPos> nearbyHoles(final BlockGetter level, final BlockPos center, final int radius) {
        List<BlockPos> holes = new ArrayList<>();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - 1; y <= center.getY() + 1; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isHole(level, pos)) {
                        holes.add(pos);
                    }
                }
            }
        }
        holes.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return holes;
    }

    public static boolean isHole(final BlockGetter level, final BlockPos pos) {
        if (level == null
                || !isOpen(level.getBlockState(pos))
                || !isOpen(level.getBlockState(pos.above()))
                || !isSafeWall(level, pos.below())) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!isSafeWall(level, pos.relative(direction))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the cells within {@code radius} that belong to a double hole: two open cells side by side, walled on the
     * six outer sides and floored with safe blocks.
     */
    public static List<BlockPos> nearbyDoubleHoleCells(
            final BlockGetter level, final BlockPos center, final int radius) {
        List<BlockPos> cells = new ArrayList<>();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - 1; y <= center.getY() + 1; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isDoubleHoleCell(level, pos)) {
                        cells.add(pos);
                    }
                }
            }
        }
        cells.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return cells;
    }

    /** Returns whether {@code pos} is one half of a double hole, open towards its partner in any direction. */
    public static boolean isDoubleHoleCell(final BlockGetter level, final BlockPos pos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (isDoubleHoleHalf(level, pos, direction)
                    && isDoubleHoleHalf(level, pos.relative(direction), direction.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDoubleHoleHalf(final BlockGetter level, final BlockPos pos, final Direction openSide) {
        if (level == null
                || !isOpen(level.getBlockState(pos))
                || !isOpen(level.getBlockState(pos.above()))
                || !isSafeWall(level, pos.below())) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (direction != openSide && !isSafeWall(level, pos.relative(direction))) {
                return false;
            }
        }
        return true;
    }

    /** Open space a player can stand in: replaceable and free of fluid such as lava or water. */
    public static boolean isOpen(final BlockState state) {
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    public static boolean isSafeWall(final BlockGetter level, final BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.BEDROCK)
                || level.getBlockState(pos).is(Blocks.OBSIDIAN)
                || level.getBlockState(pos).is(Blocks.CRYING_OBSIDIAN)
                || level.getBlockState(pos).is(Blocks.ENDER_CHEST);
    }
}
