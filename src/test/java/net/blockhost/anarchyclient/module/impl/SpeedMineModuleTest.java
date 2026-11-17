package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpeedMineModuleTest {

    @Test
    void neverStopsBelowTheServerThreshold() {
        // A 2x speed would stop at 50%, which the server rejects.
        assertFalse(SpeedMineModule.readyToStop(0.5F, 2.0F));
        assertFalse(SpeedMineModule.readyToStop(0.69F, 8.0F));
        assertTrue(SpeedMineModule.readyToStop(0.7F, 2.0F));
    }

    @Test
    void waitsForTheSpeedMultiplierAboveTheThreshold() {
        assertFalse(SpeedMineModule.readyToStop(0.75F, 1.2F));
        assertTrue(SpeedMineModule.readyToStop(0.84F, 1.2F));
        assertTrue(SpeedMineModule.readyToStop(1.0F, 1.0F));
    }
}
