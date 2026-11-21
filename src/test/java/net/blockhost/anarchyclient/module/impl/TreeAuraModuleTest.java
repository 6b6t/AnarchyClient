package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class TreeAuraModuleTest {

    private static final BlockPos POS = new BlockPos(0, 64, 0);

    @Test
    void aLoneDarkOakSaplingCannotGrow() {
        assertFalse(TreeAuraModule.canGrow(new Saplings(Set.of(POS)), POS, Blocks.DARK_OAK_SAPLING));
        assertTrue(TreeAuraModule.canGrow(new Saplings(Set.of(POS)), POS, Blocks.OAK_SAPLING));
    }

    @Test
    void aDarkOakSaplingInATwoByTwoCanGrow() {
        Set<BlockPos> group = Set.of(POS, POS.west(), POS.north(), POS.north().west());

        assertTrue(TreeAuraModule.canGrow(new Saplings(group), POS, Blocks.DARK_OAK_SAPLING));
    }

    private record Saplings(Set<BlockPos> positions) implements BlockGetter {

        @Override
        public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getBlockState(final BlockPos pos) {
            return this.positions.contains(pos)
                    ? Blocks.DARK_OAK_SAPLING.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
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
