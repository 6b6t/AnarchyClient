package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class AntiCyrillicModuleTest {

    @Test
    void transliteratesCyrillicWords() {
        assertEquals("Privet, mir!", AntiCyrillicModule.latinize("Привет, мир!"));
    }

    @Test
    void keepsCapitalizationOfMultiLetterSounds() {
        assertEquals("Shchit SHCHIT", AntiCyrillicModule.latinize("Щит ЩИТ"));
    }

    @Test
    void dropsSoftAndHardSigns() {
        assertEquals("obekt", AntiCyrillicModule.latinize("объект"));
        assertEquals("mat", AntiCyrillicModule.latinize("мать"));
    }

    @Test
    void returnsLatinTextUnchanged() {
        String message = "gg ez, see you at spawn";

        assertSame(message, AntiCyrillicModule.latinize(message));
    }
}
