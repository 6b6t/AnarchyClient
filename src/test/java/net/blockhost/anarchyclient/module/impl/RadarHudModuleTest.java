package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.world.phys.Vec2;
import org.junit.jupiter.api.Test;

class RadarHudModuleTest {

    private static final float EPSILON = 1.0E-4F;

    @Test
    void northUpRadarKeepsEastRightAndSouthDown() {
        assertOffset(5.0F, 3.0F, RadarHudModule.screenOffset(5.0, 3.0, 123.0F, false));
    }

    @Test
    void rotatedRadarPutsWhatYouFaceAtTheTop() {
        // Yaw 0 faces south (+Z), 90 west (-X), 180 north (-Z), -90 east (+X).
        assertOffset(0.0F, -10.0F, RadarHudModule.screenOffset(0.0, 10.0, 0.0F, true));
        assertOffset(0.0F, -10.0F, RadarHudModule.screenOffset(-10.0, 0.0, 90.0F, true));
        assertOffset(0.0F, -10.0F, RadarHudModule.screenOffset(0.0, -10.0, 180.0F, true));
        assertOffset(0.0F, -10.0F, RadarHudModule.screenOffset(10.0, 0.0, -90.0F, true));
    }

    @Test
    void rotatedRadarIsNotMirrored() {
        // Facing south, east (+X) is on your left and west (-X) on your right.
        assertOffset(-10.0F, 0.0F, RadarHudModule.screenOffset(10.0, 0.0, 0.0F, true));
        assertOffset(10.0F, 0.0F, RadarHudModule.screenOffset(-10.0, 0.0, 0.0F, true));
        // Facing north, east is on your right and anything south is behind you.
        assertOffset(10.0F, 0.0F, RadarHudModule.screenOffset(10.0, 0.0, 180.0F, true));
        assertOffset(0.0F, 10.0F, RadarHudModule.screenOffset(0.0, 10.0, 180.0F, true));
    }

    private static void assertOffset(final float x, final float y, final Vec2 actual) {
        assertEquals(x, actual.x, EPSILON);
        assertEquals(y, actual.y, EPSILON);
    }
}
