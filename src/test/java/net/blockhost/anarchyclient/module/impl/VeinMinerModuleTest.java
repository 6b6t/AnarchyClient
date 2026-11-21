package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class VeinMinerModuleTest {

    private static final BlockPos ORIGIN = new BlockPos(0, 10, 0);

    @Test
    void findsConnectedBlocksAroundTheBrokenOrigin() {
        TestBlockGetter level = new TestBlockGetter();
        // The origin is already broken (air) when the vein is collected.
        level.set(ORIGIN.east(), Blocks.COAL_ORE);
        level.set(ORIGIN.east(2), Blocks.COAL_ORE);
        level.set(ORIGIN.above(), Blocks.COAL_ORE);
        level.set(ORIGIN.west(), Blocks.STONE);
        level.set(ORIGIN.west(2), Blocks.COAL_ORE);

        List<BlockPos> vein = VeinMinerModule.vein(level, ORIGIN, Blocks.COAL_ORE, 10);

        assertEquals(Set.of(ORIGIN.east(), ORIGIN.east(2), ORIGIN.above()), Set.copyOf(vein));
    }

    @Test
    void stopsAtTheLimitAndPrefersNearerBlocks() {
        TestBlockGetter level = new TestBlockGetter();
        for (int x = 1; x <= 5; x++) {
            level.set(ORIGIN.east(x), Blocks.IRON_ORE);
        }

        List<BlockPos> vein = VeinMinerModule.vein(level, ORIGIN, Blocks.IRON_ORE, 3);

        assertEquals(List.of(ORIGIN.east(), ORIGIN.east(2), ORIGIN.east(3)), vein);
    }

    @Test
    void zeroLimitBreaksNothingExtra() {
        TestBlockGetter level = new TestBlockGetter();
        level.set(ORIGIN.east(), Blocks.IRON_ORE);

        assertTrue(VeinMinerModule.vein(level, ORIGIN, Blocks.IRON_ORE, 0).isEmpty());
    }

    private static final class TestBlockGetter implements BlockGetter {

        private final Map<BlockPos, BlockState> states = new HashMap<>();

        void set(final BlockPos pos, final Block block) {
            this.states.put(pos, block.defaultBlockState());
        }

        @Override
        public BlockEntity getBlockEntity(final BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(final BlockPos pos) {
            return this.states.getOrDefault(pos, Blocks.AIR.defaultBlockState());
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
