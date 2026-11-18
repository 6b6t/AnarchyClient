package net.blockhost.anarchyclient.rivet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MinecraftShapedTextTest {

    @Test
    void neverPlacesTheCursorInsideAnEmoji() {
        String text = "a😀b";

        assertEquals(1, MinecraftShapedText.codePointBoundary(text, 2));
        assertEquals(3, MinecraftShapedText.codePointBoundary(text, 3));
        assertEquals(1, MinecraftShapedText.codePointBoundary(text, 1));
    }
}
