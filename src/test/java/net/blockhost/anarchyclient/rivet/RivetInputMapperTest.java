package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.KeyEvent;
import net.lenni0451.rivet.input.keyboard.Key;
import net.lenni0451.rivet.input.keyboard.ModifierKey;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RivetInputMapperTest {

    @Test
    void mapsKnownScancodes() {
        assertEquals(Key.A, RivetInputMapper.fromScancode(InputConstants.KEY_A).orElseThrow());
        assertEquals(Key.KP_ENTER, RivetInputMapper.fromScancode(InputConstants.KEY_NUMPADENTER).orElseThrow());
    }

    @Test
    void ignoresUnknownScancodes() {
        assertTrue(RivetInputMapper.fromScancode(InputConstants.UNKNOWN.getValue()).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(Key.class)
    void roundTripsEveryRivetKey(final Key key) {
        int scancode = RivetInputMapper.toScancode(key).orElseThrow();
        assertEquals(key, RivetInputMapper.fromScancode(scancode).orElseThrow());
    }

    @Test
    void readsLettersFromTheLayoutKeycode() {
        // On AZERTY the key in QWERTY's Q position types "a", so Ctrl+that key must select all.
        var event = new KeyEvent(InputConstants.KEY_Q, InputConstants.KEYCODE_A, InputConstants.MOD_CONTROL);
        var mapped = RivetInputMapper.key(event).orElseThrow();

        assertEquals(Key.A, mapped.key());
        assertEquals(Set.of(ModifierKey.CONTROL), mapped.modifiers());
    }

    @Test
    void readsNonLetterKeysFromTheScancode() {
        var event = new KeyEvent(InputConstants.KEY_BACKSPACE, 8, 0);

        assertEquals(Key.BACKSPACE, RivetInputMapper.key(event).orElseThrow().key());
    }

    @ParameterizedTest
    @CsvSource({"1,SHIFT", "2,SHIFT", "64,CONTROL", "128,CONTROL", "256,ALT", "512,ALT", "1024,SUPER", "2048,SUPER"})
    void mapsLeftAndRightSdlModifiers(final int mask, final ModifierKey modifier) {
        assertEquals(Set.of(modifier), RivetInputMapper.modifiers(mask));
    }

    @ParameterizedTest
    @CsvSource({"1,LEFT", "2,MIDDLE", "3,RIGHT"})
    void mapsSdlMouseButtons(final int button, final MouseButton expected) {
        var event = new MouseButtonEvent(12.0, 34.0, new MouseButtonInfo(button, InputConstants.MOD_SHIFT));
        var mapped = RivetInputMapper.mouse(event).orElseThrow();

        assertEquals(expected, mapped.button());
        assertEquals(12.0F, mapped.x());
        assertEquals(34.0F, mapped.y());
        assertEquals(Set.of(ModifierKey.SHIFT), mapped.modifiers());
    }

    @ParameterizedTest
    @CsvSource({"4", "5", "6", "7", "8"})
    void ignoresSideMouseButtons(final int button) {
        var event = new MouseButtonEvent(12.0, 34.0, new MouseButtonInfo(button, 0));

        assertTrue(RivetInputMapper.mouse(event).isEmpty());
    }
}
