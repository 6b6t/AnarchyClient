package net.blockhost.anarchyclient.module.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageEstimatorTest {

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
}
