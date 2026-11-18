package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BreakDelayModuleTest {

    @Test
    void onlyShortensTheDelaySoItCanCountDown() {
        // Vanilla sets 4 after a use and repeats when it reaches 0; the module must not reset it every tick.
        assertEquals(2, BreakDelayModule.shortenedDelay(4, 2));
        assertEquals(1, BreakDelayModule.shortenedDelay(1, 2));
        assertEquals(0, BreakDelayModule.shortenedDelay(0, 2));
    }
}
