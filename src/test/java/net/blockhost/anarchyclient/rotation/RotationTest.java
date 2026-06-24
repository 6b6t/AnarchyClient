package net.blockhost.anarchyclient.rotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RotationTest {

    @Test
    void wrapsDegreesToShortestTurn() {
        assertEquals(20.0F, Rotation.wrapDegrees(-340.0F));
        assertEquals(-20.0F, Rotation.wrapDegrees(340.0F));
    }

    @Test
    void stepsTowardTargetWithoutOvershooting() {
        Rotation stepped = new Rotation(170.0F, 0.0F).stepToward(new Rotation(-170.0F, 45.0F), 15.0F);

        assertEquals(185.0F, stepped.yaw());
        assertEquals(15.0F, stepped.pitch());
    }

    @Test
    void clampsPitch() {
        assertEquals(90.0F, new Rotation(0.0F, 120.0F).clampPitch().pitch());
        assertEquals(-90.0F, new Rotation(0.0F, -120.0F).clampPitch().pitch());
    }

    @Test
    void linearStepMovesAlongCombinedAngle() {
        Rotation stepped = new Rotation(0.0F, 0.0F).linearStepToward(new Rotation(30.0F, 40.0F), 10.0F);

        assertEquals(6.0F, stepped.yaw(), 0.0001F);
        assertEquals(8.0F, stepped.pitch(), 0.0001F);
    }
}
