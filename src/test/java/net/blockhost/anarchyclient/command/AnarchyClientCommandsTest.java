package net.blockhost.anarchyclient.command;

import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnarchyClientCommandsTest {

    @Test
    void parsesBooleanSettingValues() {
        BooleanSetting setting = BooleanSetting.from(BooleanSetting.builder()
                .id("enabled")
                .name("Enabled")
                .defaultValue(false)
                .build());

        AnarchyClientCommands.setSettingValue(setting, "true");

        assertEquals(true, setting.value());
        assertThrows(IllegalArgumentException.class, () -> AnarchyClientCommands.setSettingValue(setting, "yes"));
    }

    @Test
    void parsesNumberSettingValuesThroughSettingSanitizer() {
        NumberSetting setting = NumberSetting.from(NumberSetting.builder()
                .id("range")
                .name("Range")
                .defaultValue(1.0)
                .min(0.0)
                .max(6.0)
                .step(0.5)
                .build());

        AnarchyClientCommands.setSettingValue(setting, "9");

        assertEquals(6.0, setting.value());
        assertThrows(IllegalArgumentException.class, () -> AnarchyClientCommands.setSettingValue(setting, "far"));
    }

    @Test
    void validatesSelectSettingValues() {
        SelectSetting setting = SelectSetting.from(SelectSetting.builder()
                .id("mode")
                .name("Mode")
                .defaultValue("First")
                .addAllOptions(List.of("First", "Second"))
                .build());

        AnarchyClientCommands.setSettingValue(setting, "Second");

        assertEquals("Second", setting.value());
        assertThrows(IllegalArgumentException.class, () -> AnarchyClientCommands.setSettingValue(setting, "Third"));
    }

    @Test
    void acceptsStringSettingValues() {
        StringSetting setting = StringSetting.from(StringSetting.builder()
                .id("message")
                .name("Message")
                .defaultValue("one")
                .build());

        AnarchyClientCommands.setSettingValue(setting, "two words");

        assertEquals("two words", setting.value());
    }

    @Test
    void centerModesSnapCoordinatesPredictably() {
        assertEquals(12.5, AnarchyClientCommands.CenterMode.MIDDLE.coordinate(12.2));
        assertEquals(-4.5, AnarchyClientCommands.CenterMode.MIDDLE.coordinate(-4.8));
        assertEquals(12.0, AnarchyClientCommands.CenterMode.CORNER.coordinate(12.2));
        assertEquals(-5.0, AnarchyClientCommands.CenterMode.CORNER.coordinate(-4.8));
    }
}
