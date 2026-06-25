package net.blockhost.anarchyclient.timer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimerManagerTest {

    @AfterEach
    void clearTimerRequests() {
        TimerManager.clearAll();
    }

    @Test
    void highestPriorityMultiplierAdjustsMspt() {
        TimerManager.request("slow", 0.5, TimerManager.PRIORITY_NORMAL, 5);
        TimerManager.request("fast", 2.0, TimerManager.PRIORITY_MOVEMENT, 5);

        assertEquals(2.0, TimerManager.multiplier());
        assertEquals(25.0F, TimerManager.adjustMspt(50.0F), 1.0E-6F);
    }

    @Test
    void requestsExpireAfterTicks() {
        TimerManager.request("fast", 2.0, TimerManager.PRIORITY_MOVEMENT, 1);

        TimerManager.tick();

        assertTrue(TimerManager.activeMultiplier().isEmpty());
        assertEquals(1.0, TimerManager.multiplier());
    }

    @Test
    void clampsInvalidMultipliers() {
        assertEquals(1.0, TimerManager.clampMultiplier(Double.NaN));
        assertEquals(0.1, TimerManager.clampMultiplier(-5.0));
        assertEquals(10.0, TimerManager.clampMultiplier(20.0));
    }
}
