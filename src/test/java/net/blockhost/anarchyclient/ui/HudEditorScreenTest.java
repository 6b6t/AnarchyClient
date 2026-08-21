package net.blockhost.anarchyclient.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void pinsToZeroWhenElementIsLargerThanScreen() {
        assertEquals(0, HudEditorScreen.snap(0, SCREEN + 20, SCREEN));
        assertEquals(0, HudEditorScreen.snap(50, SCREEN, SCREEN));
    }

    @Test
    void clampsWithinScreenBounds() {
        // Dragged past the right edge: clamped to the edge, then snapped to the 6px inset (354).
        assertEquals(354, HudEditorScreen.snap(999, SIZE, SCREEN));
        assertEquals(6, HudEditorScreen.snap(-50, SIZE, SCREEN)); // clamps to 0, then snaps to left margin
    }

    @Test
    void groupKeepsItsSpacingAgainstEveryEdge() {
        // Two elements 100px apart, dragged past each edge in turn. Whatever the shift, the gap holds.
        int gap = 100;
        for (int leading : new int[]{-80, 0, 9, 250, 999}) {
            int trailing = leading + gap;
            int shift = HudEditorScreen.groupShift(leading, trailing + SIZE, SCREEN);
            int first = leading + shift;
            int second = trailing + shift;
            assertEquals(gap, second - first, "spacing must survive the clamp");
            assertTrue(first >= 0 && second + SIZE <= SCREEN, "group must land fully on screen");
        }
    }

    @Test
    void groupShiftLeavesAFittingGroupAlone() {
        assertEquals(0, HudEditorScreen.groupShift(9, 200, SCREEN));
    }

    @Test
    void groupWiderThanScreenPinsItsLeadingEdge() {
        // Nothing can bring both edges in, so the near edge wins and the rest overflows.
        assertEquals(-20, HudEditorScreen.groupShift(20, SCREEN + 80, SCREEN));
    }

    @Test
    void selectionBoxTakesEveryTouchedElement() {
        HudLayout.Element element = new HudLayout.Element("id", "Name", 100, 100, 40, 20);
        assertTrue(element.intersects(0, 0, 400, 400), "fully enclosed");
        assertTrue(element.intersects(130, 110, 300, 300), "overlapping corner counts");
        assertTrue(element.intersects(140, 120, 300, 300), "touching the bottom-right edge counts");
        assertFalse(element.intersects(141, 121, 300, 300), "just past the element misses");
        assertFalse(element.intersects(0, 0, 99, 400), "left of the element misses");
    }
}
