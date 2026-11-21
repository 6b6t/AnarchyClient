package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class DamageEstimatorTest {

    private static final BlockPos ANCHOR = new BlockPos(0, 64, 0);
    private static final AABB PLAYER_BOX = new AABB(2.2, 64.0, 0.2, 2.8, 65.8, 0.8);

    @Test
    void explodingBlockShieldsEveryRayUnlessRemoved() {
        BlockGetter level = new MapBlockGetter(Map.of(ANCHOR, Blocks.RESPAWN_ANCHOR.defaultBlockState()));
        Vec3 center = DamageEstimator.blockCenter(ANCHOR);

        assertEquals(0.0F, DamageEstimator.seenPercent(level, center, PLAYER_BOX, CollisionContext.empty()));
        BlockGetter removed = new DamageEstimator.RemovedBlocksGetter(level, Set.of(ANCHOR));
        assertEquals(1.0F, DamageEstimator.seenPercent(removed, center, PLAYER_BOX, CollisionContext.empty()));
    }

    @Test
    void removedBlocksKeepOtherCover() {
        BlockPos wall = new BlockPos(1, 64, 0);
        BlockPos wallAbove = wall.above();
        BlockGetter level = new MapBlockGetter(Map.of(
                ANCHOR, Blocks.RESPAWN_ANCHOR.defaultBlockState(),
                wall, Blocks.OBSIDIAN.defaultBlockState(),
                wallAbove, Blocks.OBSIDIAN.defaultBlockState()));
        BlockGetter removed = new DamageEstimator.RemovedBlocksGetter(level, Set.of(ANCHOR));

        assertEquals(
                0.0F,
                DamageEstimator.seenPercent(
                        removed, DamageEstimator.blockCenter(ANCHOR), PLAYER_BOX, CollisionContext.empty()));
    }

    @Test
    void matchesVanillaCrystalDamageCurve() {
        // ExplosionDamageCalculator: (p^2 + p) / 2 * 7 * (2 * power) + 1 with p = (1 - d / (2 * power)) * exposure.
        assertEquals(85.0, DamageEstimator.rawDamage(0.0, DamageEstimator.CRYSTAL_POWER, 1.0), 1.0E-9);
        assertEquals(32.5, DamageEstimator.rawDamage(6.0, DamageEstimator.CRYSTAL_POWER, 1.0), 1.0E-9);
        assertEquals(1.0, DamageEstimator.rawDamage(12.0, DamageEstimator.CRYSTAL_POWER, 1.0), 1.0E-9);
        assertEquals(0.0, DamageEstimator.rawDamage(12.01, DamageEstimator.CRYSTAL_POWER, 1.0));
    }

    @Test
    void scalesWithExposureAndPower() {
        assertEquals(1.0, DamageEstimator.rawDamage(0.0, DamageEstimator.CRYSTAL_POWER, 0.0), 1.0E-9);
        // Power 5 (anchor, bed): radius 10, half exposure at the center gives p = 0.5.
        assertEquals(0.375 * 70.0 + 1.0, DamageEstimator.rawDamage(0.0, DamageEstimator.ANCHOR_POWER, 0.5), 1.0E-9);
        assertEquals(0.0, DamageEstimator.rawDamage(0.0, 0.0, 1.0));
    }

    @Test
    void appliesDifficultyLikePlayerHurtServer() {
        assertEquals(0.0, DamageEstimator.scaleForDifficulty(10.0, Difficulty.PEACEFUL));
        assertEquals(6.0, DamageEstimator.scaleForDifficulty(10.0, Difficulty.EASY));
        assertEquals(1.0, DamageEstimator.scaleForDifficulty(1.0, Difficulty.EASY));
        assertEquals(10.0, DamageEstimator.scaleForDifficulty(10.0, Difficulty.NORMAL));
        assertEquals(15.0, DamageEstimator.scaleForDifficulty(10.0, Difficulty.HARD));
    }

    @Test
    void appliesArmorAndResistanceLikeVanilla() {
        // Full netherite: armor 20, toughness 12. toughness factor 5, real armor clamp(20 - 4, 4, 20) = 16.
        assertEquals(20.0 * (1.0 - 16.0 / 25.0), DamageEstimator.afterArmor(20.0, 20.0, 12.0), 1.0E-9);
        // Huge hits fall to the 20 % armor floor.
        assertEquals(100.0 * (1.0 - 4.0 / 25.0), DamageEstimator.afterArmor(100.0, 20.0, 0.0), 1.0E-9);
        assertEquals(10.0, DamageEstimator.afterArmor(10.0, 0.0, 0.0), 1.0E-9);

        assertEquals(10.0, DamageEstimator.afterResistance(10.0, -1));
        assertEquals(8.0, DamageEstimator.afterResistance(10.0, 0), 1.0E-9);
        assertEquals(0.0, DamageEstimator.afterResistance(10.0, 4), 1.0E-9);
    }

    @Test
    void crystalsExplodeAtTheBottomCenterOfTheirBlock() {
        assertEquals(new Vec3(1.5, 2.0, 3.5), DamageEstimator.crystalCenter(new BlockPos(1, 2, 3)));
        assertEquals(new Vec3(1.5, 2.5, 3.5), DamageEstimator.blockCenter(new BlockPos(1, 2, 3)));
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
