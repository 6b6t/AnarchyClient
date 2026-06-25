package net.blockhost.anarchyclient.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HoleManager {

    private HoleManager() {
    }

    public static List<BlockPos> nearbyHoles(final ClientLevel level, final BlockPos center, final int radius) {
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

    public static boolean isHole(final ClientLevel level, final BlockPos pos) {
        if (level == null
                || !level.getBlockState(pos).canBeReplaced()
                || !level.getBlockState(pos.above()).canBeReplaced()
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

    public static boolean isSafeWall(final ClientLevel level, final BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.BEDROCK)
                || level.getBlockState(pos).is(Blocks.OBSIDIAN)
                || level.getBlockState(pos).is(Blocks.CRYING_OBSIDIAN)
                || level.getBlockState(pos).is(Blocks.ENDER_CHEST);
    }
}
