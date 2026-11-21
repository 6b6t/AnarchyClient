package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ReverseStepModuleTest {

    @Test
    void jumpStaysMarkedAsRisingPastItsPeak() {
        boolean rising = ReverseStepModule.airborneAfterRising(false, false, 0.42);
        assertTrue(rising);
        rising = ReverseStepModule.airborneAfterRising(rising, false, -0.08);
        assertTrue(rising);
        assertFalse(ReverseStepModule.airborneAfterRising(rising, true, 0.0));
    }

    @Test
    void walkingOffALedgeIsNotRising() {
        assertFalse(ReverseStepModule.airborneAfterRising(false, false, -0.08));
    }
}
