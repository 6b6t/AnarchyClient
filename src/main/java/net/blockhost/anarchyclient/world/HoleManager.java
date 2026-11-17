package net.blockhost.anarchyclient.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HoleManager {

    private HoleManager() {
    }

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
