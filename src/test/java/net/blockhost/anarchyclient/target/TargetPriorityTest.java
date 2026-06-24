package net.blockhost.anarchyclient.target;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TargetPriorityTest {

    @Test
    void mapsUiLabelsToPriorities() {
        assertEquals(TargetPriority.TYPE, TargetPriority.fromSetting("Type"));
        assertEquals(TargetPriority.HEALTH, TargetPriority.fromSetting("Lowest HP"));
        assertEquals(TargetPriority.ARMOR, TargetPriority.fromSetting("Lowest Armor"));
        assertEquals(TargetPriority.CROSSHAIR, TargetPriority.fromSetting("Crosshair"));
        assertEquals(TargetPriority.AGE, TargetPriority.fromSetting("Age"));
        assertEquals(TargetPriority.DISTANCE, TargetPriority.fromSetting("Nearest"));
    }
}
