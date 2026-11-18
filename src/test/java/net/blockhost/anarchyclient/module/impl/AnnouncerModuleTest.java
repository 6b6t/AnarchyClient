package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnnouncerModuleTest {

    @Test
    void announcesNothingWithoutActivity() {
        assertNull(AnnouncerModule.announcement(0, 0, 0));
        assertNull(AnnouncerModule.announcement(9, 0, 0));
    }

    @Test
    void walkingWinsOnlyWhenClearlyLarger() {
        assertEquals("I just walked 300 blocks.", AnnouncerModule.announcement(300, 20, 5));
        assertEquals("I just placed 40 blocks.", AnnouncerModule.announcement(300, 40, 5));
    }

    @Test
    void usesSingularForOneBlock() {
        assertEquals("I just mined 1 block.", AnnouncerModule.announcement(0, 0, 1));
        assertEquals("I just placed 1 block.", AnnouncerModule.announcement(0, 1, 1));
    }
}
