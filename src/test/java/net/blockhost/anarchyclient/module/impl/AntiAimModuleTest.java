package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AntiAimModuleTest {

    @Test
    void spinAdvancesYawEachTickAndWraps() {
        assertEquals(30.0F, AntiAimModule.yaw("Spin", 0.0F, 1, 30.0F, 0.0F));
        assertEquals(-150.0F, AntiAimModule.yaw("Spin", 0.0F, 7, 30.0F, 0.0F));
    }

    @Test
    void jitterAndBackwardsFollowTheRealYaw() {
        assertEquals(100.0F, AntiAimModule.yaw("Jitter", 10.0F, 2, 0.0F, 0.0F));
        assertEquals(-80.0F, AntiAimModule.yaw("Jitter", 10.0F, 3, 0.0F, 0.0F));
        assertEquals(-170.0F, AntiAimModule.yaw("Backwards", 10.0F, 0, 0.0F, 0.0F));
    }

    @Test
    void offModesKeepTheRealRotation() {
        assertEquals(42.0F, AntiAimModule.yaw("Off", 42.0F, 5, 30.0F, 0.0F));
        assertEquals(-12.0F, AntiAimModule.pitch("Off", -12.0F, 5, 0.0F));
    }

    @Test
    void pitchModes() {
        assertEquals(90.0F, AntiAimModule.pitch("Down", 0.0F, 0, 0.0F));
        assertEquals(-90.0F, AntiAimModule.pitch("Up", 0.0F, 0, 0.0F));
        assertEquals(-90.0F, AntiAimModule.pitch("Jitter", 0.0F, 1, 0.0F));
        assertEquals(25.0F, AntiAimModule.pitch("Static", 0.0F, 1, 25.0F));
    }
}
