package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AntiHungerModuleTest {

    @Test
    void serverFallDistanceGrowsWhileAirborneAndResetsOnGround() {
        double distance = AntiHungerModule.nextServerFallDistance(0.0, 0.5, false);
        distance = AntiHungerModule.nextServerFallDistance(distance, 0.75, false);
        assertEquals(1.25, distance, 1.0E-9);
        assertEquals(0.0, AntiHungerModule.nextServerFallDistance(distance, 0.5, true));
    }

    @Test
    void stopsSpoofingBeforeTheServerFallDistanceCanHurt() {
        assertTrue(AntiHungerModule.canSpoof(0.0, 0.5));
        assertTrue(AntiHungerModule.canSpoof(1.25, 0.5));
        assertFalse(AntiHungerModule.canSpoof(1.5, 0.5));
        assertFalse(AntiHungerModule.canSpoof(0.0, AntiHungerModule.MAX_SPOOFED_DROP));
    }
}
