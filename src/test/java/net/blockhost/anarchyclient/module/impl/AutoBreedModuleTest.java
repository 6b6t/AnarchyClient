package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AutoBreedModuleTest {

    @Test
    void waitsAFullBreedingCycleBeforeFeedingAgain() {
        assertTrue(AutoBreedModule.recentlyFed(1_000L, 1_000L + 6_599L));
        assertFalse(AutoBreedModule.recentlyFed(1_000L, 1_000L + 6_600L));
    }

    @Test
    void forgetsFeedingsFromBeforeATimeReset() {
        assertFalse(AutoBreedModule.recentlyFed(5_000L, 100L));
    }
}
