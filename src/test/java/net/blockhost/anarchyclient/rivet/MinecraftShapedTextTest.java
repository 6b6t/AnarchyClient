package net.blockhost.anarchyclient.rivet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MinecraftShapedTextTest {

    @Test
    void neverPlacesTheCursorInsideAnEmoji() {
        String text = "a😀b";

        assertEquals(1, MinecraftShapedText.codePointBoundary(text, 2));
        assertEquals(3, MinecraftShapedText.codePointBoundary(text, 3));
        assertEquals(1, MinecraftShapedText.codePointBoundary(text, 1));
    }
}
