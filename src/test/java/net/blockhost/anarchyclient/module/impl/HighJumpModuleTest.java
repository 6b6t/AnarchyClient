package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HighJumpModuleTest {

    @Test
    void vanillaHeightKeepsTheVanillaJump() {
        assertEquals(0.3332, HighJumpModule.boostedVelocity(0.3332, 0.42), 1.0E-9);
    }

    @Test
    void higherSettingAddsTheExtraLaunchSpeedAfterDrag() {
        // A 0.75 launch after one tick of gravity and drag: (0.75 - 0.08) * 0.98.
        assertEquals((0.75 - 0.08) * 0.98, HighJumpModule.boostedVelocity((0.42 - 0.08) * 0.98, 0.75), 1.0E-9);
    }
}
