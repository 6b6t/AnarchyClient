package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.clock.ClockNetworkState;
import org.junit.jupiter.api.Test;

class TimeChangerModuleTest {

    @Test
    void advancesTheSavedServerClockByElapsedGameTicks() {
        ClockNetworkState restored = TimeChangerModule.advance(new ClockNetworkState(1000L, 0.0F, 1.0F), 40L);

        assertEquals(new ClockNetworkState(1040L, 0.0F, 1.0F), restored);
    }

    @Test
    void keepsAPausedServerClockAtItsSavedTime() {
        ClockNetworkState restored = TimeChangerModule.advance(new ClockNetworkState(6000L, 0.25F, 0.0F), 200L);

        assertEquals(new ClockNetworkState(6000L, 0.25F, 0.0F), restored);
    }

    @Test
    void carriesFractionalProgressForSlowClocks() {
        ClockNetworkState restored = TimeChangerModule.advance(new ClockNetworkState(100L, 0.5F, 0.5F), 3L);

        assertEquals(new ClockNetworkState(102L, 0.0F, 0.5F), restored);
    }
}
