package net.blockhost.anarchyclient.test;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/**
 * A block getter backed by a map, with air everywhere else, for tests that need a small world.
 */
public final class MapBlockGetter implements BlockGetter {

    private final Map<BlockPos, BlockState> blocks = new HashMap<>();

    public MapBlockGetter set(final BlockPos pos, final Block block) {
        this.blocks.put(pos.immutable(), block.defaultBlockState());
        return this;
    }

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
