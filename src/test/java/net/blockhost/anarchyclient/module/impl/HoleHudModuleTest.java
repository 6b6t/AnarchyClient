package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
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
class HoleHudModuleTest {

    private static final BlockPos FEET = new BlockPos(0, 64, 0);

    @Test
    void obsidianHoleIsSafe() {
        assertTrue(HoleEspModule.isHole(surroundedBy(Blocks.OBSIDIAN), FEET));
    }

    @Test
    void mixedBedrockAndObsidianHoleIsSafe() {
        TestBlockGetter level = surroundedBy(Blocks.OBSIDIAN);
        level.set(FEET.below(), Blocks.BEDROCK);
        level.set(FEET.east(), Blocks.BEDROCK);

        assertTrue(HoleEspModule.isHole(level, FEET));
    }

    @Test
    void stoneHoleIsNotSafeEvenThoughItIsSolid() {
        assertFalse(HoleEspModule.isHole(surroundedBy(Blocks.STONE), FEET));
    }

    @Test
    void oneBreakableWallMakesTheHoleUnsafe() {
        TestBlockGetter level = surroundedBy(Blocks.OBSIDIAN);
        level.set(FEET.north(), Blocks.COBBLESTONE);

        assertFalse(HoleEspModule.isHole(level, FEET));
    }

    private static TestBlockGetter surroundedBy(final Block block) {
        TestBlockGetter level = new TestBlockGetter();
        level.set(FEET.below(), block);
        level.set(FEET.north(), block);
        level.set(FEET.south(), block);
        level.set(FEET.east(), block);
        level.set(FEET.west(), block);
        return level;
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
