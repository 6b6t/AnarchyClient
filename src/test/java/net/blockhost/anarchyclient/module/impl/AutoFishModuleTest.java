package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AutoFishModuleTest {

    @Test
    void recastCooldownWaitsTenTicksAndStaysReadyUntilReset() {
        AutoFishModule module = new AutoFishModule();

        assertTrue(module.isRecastReady());

        module.resetRecastCooldown();
        for (int tick = 0; tick < 10; tick++) {
            assertFalse(module.isRecastReady());
        }

        assertTrue(module.isRecastReady());
        assertTrue(module.isRecastReady());
    }
}
