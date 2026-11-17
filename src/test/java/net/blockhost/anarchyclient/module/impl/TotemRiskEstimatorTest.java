package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TotemRiskEstimatorTest {

    @Test
    void reportsHealthLeftAboveThreshold() {
        assertEquals(6.0, TotemRiskEstimator.damageUntilThreshold(10.0, 4.0));
        assertEquals(0.0, TotemRiskEstimator.damageUntilThreshold(3.0, 4.0));
    }
}
