package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmbienceModuleTest {

    @Test
    void packsAndClampsColorChannels() {
        assertEquals(0x80FF4000, AmbienceModule.argb(128, 255, 64, 0));
        assertEquals(0xFFFF0000, AmbienceModule.argb(300, 999, -5, -1));
    }
}
