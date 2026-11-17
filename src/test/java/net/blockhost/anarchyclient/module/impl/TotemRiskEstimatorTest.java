package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TotemRiskEstimatorTest {

    @Test
    void scalesExplosionDamageWithDistance() {
        assertEquals(20.0, TotemRiskEstimator.estimateExplosionDamage(0.0, 12.0, 20.0), 1.0E-9);
        assertEquals(7.5, TotemRiskEstimator.estimateExplosionDamage(36.0, 12.0, 20.0), 1.0E-9);
        assertEquals(0.0, TotemRiskEstimator.estimateExplosionDamage(144.0, 12.0, 20.0), 1.0E-9);
        assertEquals(0.0, TotemRiskEstimator.estimateExplosionDamage(400.0, 12.0, 20.0), 1.0E-9);
    }

    @Test
    void ignoresInvalidExplosionInputs() {
        assertEquals(0.0, TotemRiskEstimator.estimateExplosionDamage(0.0, 0.0, 20.0));
        assertEquals(0.0, TotemRiskEstimator.estimateExplosionDamage(0.0, 12.0, 0.0));
    }

    @Test
    void reportsHealthLeftAboveThreshold() {
        assertEquals(6.0, TotemRiskEstimator.damageUntilThreshold(10.0, 4.0));
        assertEquals(0.0, TotemRiskEstimator.damageUntilThreshold(3.0, 4.0));
    }
}
