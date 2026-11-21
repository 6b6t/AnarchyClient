package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class KeepSprintModuleTest {

    @Test
    void undoesTheSprintHitSlowdownAndKeepsVerticalMotion() {
        Vec3 before = new Vec3(0.3, 0.0, -0.2);
        Vec3 slowed = new Vec3(0.18, -0.08, -0.12);

        assertEquals(new Vec3(0.3, -0.08, -0.2), KeepSprintModule.restoredMotion(before, slowed));
    }

    @Test
    void neverAddsSpeedTheHitDidNotTake() {
        Vec3 before = new Vec3(0.1, 0.0, 0.0);
        Vec3 now = new Vec3(0.4, 0.0, 0.0);

        assertEquals(now, KeepSprintModule.restoredMotion(before, now));
    }
}
