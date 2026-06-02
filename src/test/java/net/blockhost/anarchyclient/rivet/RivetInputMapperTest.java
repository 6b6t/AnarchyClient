package net.blockhost.anarchyclient.rivet;

import net.lenni0451.rivet.input.keyboard.Key;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RivetInputMapperTest {

    @Test
    void mapsKnownGlfwKeys() {
        assertEquals(Key.A, RivetInputMapper.fromGlfw(GLFW.GLFW_KEY_A).orElseThrow());
        assertEquals(Key.KP_ENTER, RivetInputMapper.fromGlfw(GLFW.GLFW_KEY_KP_ENTER).orElseThrow());
    }

    @Test
    void ignoresUnknownGlfwKeys() {
        assertTrue(RivetInputMapper.fromGlfw(GLFW.GLFW_KEY_UNKNOWN).isEmpty());
    }

    @Test
    void mapsRivetKeysBackToGlfw() {
        assertEquals(GLFW.GLFW_KEY_RIGHT_SUPER, RivetInputMapper.toGlfw(Key.RIGHT_SUPER).orElseThrow());
        assertEquals(GLFW.GLFW_KEY_SPACE, RivetInputMapper.toGlfw(Key.SPACE).orElseThrow());
    }
}
