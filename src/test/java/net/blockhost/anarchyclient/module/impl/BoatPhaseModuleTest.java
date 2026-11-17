package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoatPhaseModuleTest {

    @Test
    void idleVehicleHoversInsteadOfSinking() {
        assertEquals(0.0, BoatPhaseModule.verticalSpeed(false, false, 0.25));
        assertEquals(0.0, BoatPhaseModule.verticalSpeed(true, true, 0.25));
    }

    @Test
    void jumpAndSneakMoveTheVehicleVertically() {
        assertEquals(0.25, BoatPhaseModule.verticalSpeed(true, false, 0.25));
        assertEquals(-0.25, BoatPhaseModule.verticalSpeed(false, true, 0.25));
    }
}
