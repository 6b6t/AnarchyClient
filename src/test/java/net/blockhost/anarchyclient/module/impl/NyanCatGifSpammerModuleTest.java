package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NyanCatGifSpammerModuleTest {

    @Test
    void convertsSecondsToTicks() {
        assertEquals(20, NyanCatGifSpammerModule.secondsToTicks(1.0));
        assertEquals(50, NyanCatGifSpammerModule.secondsToTicks(2.5));
        assertEquals(1, NyanCatGifSpammerModule.secondsToTicks(0.0));
    }

    @Test
    void randomIntervalUsesInclusiveTickRange() {
        Random random = new Random(1);

        for (int index = 0; index < 100; index++) {
            int ticks = NyanCatGifSpammerModule.randomIntervalTicks(3.0, 5.0, random);
            assertTrue(ticks >= 60 && ticks <= 100);
        }
    }

    @Test
    void randomIntervalHandlesReversedSettings() {
        Random random = new Random(1);

        for (int index = 0; index < 100; index++) {
            int ticks = NyanCatGifSpammerModule.randomIntervalTicks(5.0, 3.0, random);
            assertTrue(ticks >= 60 && ticks <= 100);
        }
    }

    @Test
    void chatSpammerParsesPipeSeparatedMessages() {
        assertEquals(
                java.util.List.of("hello", "gg", "bye"),
                ChatSpammerModule.parseMessages(" hello | | gg | bye ")
        );
    }

    @Test
    void autoEatTriggersForLowHungerOrHealth() {
        assertTrue(SmartEatModule.shouldEat(10, 20.0F, 14.0, 10.0));
        assertTrue(SmartEatModule.shouldEat(20, 8.0F, 14.0, 10.0));
        assertEquals(false, SmartEatModule.shouldEat(20, 20.0F, 14.0, 10.0));
    }
}
