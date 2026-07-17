package net.blockhost.anarchyclient.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HudEditorScreenTest {

    private static final int SCREEN = 400;
    private static final int SIZE = 40;

    @Test
    void snapsToLeftEdgeWithinThreshold() {
        assertEquals(6, HudEditorScreen.snap(9, SIZE, SCREEN));
    }

    @Test
    void snapsToRightEdgeWithinThreshold() {
        // Right inset target is screen - margin - size = 354.
        assertEquals(354, HudEditorScreen.snap(352, SIZE, SCREEN));
    }

    @Test
    void snapsToCenterWithinThreshold() {
        int centered = (SCREEN - SIZE) / 2; // 180
        assertEquals(centered, HudEditorScreen.snap(centered + 3, SIZE, SCREEN));
    }

    @Test
    void keepsFreePositionOutsideThresholds() {
        assertEquals(100, HudEditorScreen.snap(100, SIZE, SCREEN));
    }

    @Test
    void clampsWithinScreenBounds() {
        // Dragged past the right edge: clamped to the edge, then snapped to the 6px inset (354).
        assertEquals(354, HudEditorScreen.snap(999, SIZE, SCREEN));
        assertEquals(6, HudEditorScreen.snap(-50, SIZE, SCREEN)); // clamps to 0, then snaps to left margin
    }
}
