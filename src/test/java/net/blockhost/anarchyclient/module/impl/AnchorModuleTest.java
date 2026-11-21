package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class AnchorModuleTest {

    private static final BlockPos HOLE = new BlockPos(0, 64, 0);

    @Test
    void findsHoleBelowThroughOpenAir() {
        assertEquals(Optional.of(HOLE), AnchorModule.holeBelow(walledHole(), HOLE.above(3), 5));
    }

    @Test
    void ignoresFlatGroundSoNormalJumpsAreNotPulledDown() {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(HOLE.below(), Blocks.STONE.defaultBlockState());

        assertEquals(Optional.empty(), AnchorModule.holeBelow(new MapBlockGetter(blocks), HOLE.above(), 5));
    }

    @Test
    void ignoresHolesDeeperThanTheSearchDepthOrBehindBlocks() {
        assertEquals(Optional.empty(), AnchorModule.holeBelow(walledHole(), HOLE.above(4), 3));

        Map<BlockPos, BlockState> blocks = holeBlocks();
        blocks.put(HOLE.above(2), Blocks.STONE.defaultBlockState());
        assertEquals(Optional.empty(), AnchorModule.holeBelow(new MapBlockGetter(blocks), HOLE.above(3), 5));
    }

    private static BlockGetter walledHole() {
        return new MapBlockGetter(holeBlocks());
    }

    private static Map<BlockPos, BlockState> holeBlocks() {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(HOLE.below(), Blocks.OBSIDIAN.defaultBlockState());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            blocks.put(HOLE.relative(direction), Blocks.BEDROCK.defaultBlockState());
        }
        return blocks;
    }

    private record MapBlockGetter(Map<BlockPos, BlockState> blocks) implements BlockGetter {

        @Override
        public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(final BlockPos pos) {
            return this.blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public FluidState getFluidState(final BlockPos pos) {
            return this.getBlockState(pos).getFluidState();
        }

        @Override
        public int getHeight() {
            return 384;
        }

        @Override
        public int getMinY() {
            return -64;
        }
    }
}
