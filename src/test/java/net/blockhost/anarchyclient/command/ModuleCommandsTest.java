package net.blockhost.anarchyclient.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import org.junit.jupiter.api.Test;

class ModuleCommandsTest {

    @Test
    void parsesBooleanSettingValues() {
        BooleanSetting setting = BooleanSetting.from(BooleanSetting.builder()
                .id("enabled")
                .name("Enabled")
                .defaultValue(false)
                .build());

        ModuleCommands.setSettingValue(setting, "true");

        assertEquals(true, setting.value());
        assertThrows(IllegalArgumentException.class, () -> ModuleCommands.setSettingValue(setting, "yes"));
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

        ModuleCommands.setSettingValue(setting, "9");

        assertEquals(6.0, setting.value());
        assertThrows(IllegalArgumentException.class, () -> ModuleCommands.setSettingValue(setting, "far"));
        assertThrows(IllegalArgumentException.class, () -> ModuleCommands.setSettingValue(setting, "NaN"));
        assertThrows(IllegalArgumentException.class, () -> ModuleCommands.setSettingValue(setting, "Infinity"));
        assertEquals(6.0, setting.value());
    }

    @Test
    void validatesSelectSettingValues() {
        SelectSetting setting = SelectSetting.from(SelectSetting.builder()
                .id("mode")
                .name("Mode")
                .defaultValue("First")
                .addAllOptions(List.of("First", "Second"))
                .build());

        ModuleCommands.setSettingValue(setting, "Second");

        assertEquals("Second", setting.value());
        assertThrows(IllegalArgumentException.class, () -> ModuleCommands.setSettingValue(setting, "Third"));
    }

    @Test
    void acceptsStringSettingValues() {
        StringSetting setting = StringSetting.from(StringSetting.builder()
                .id("message")
                .name("Message")
                .defaultValue("one")
                .build());

        ModuleCommands.setSettingValue(setting, "two words");

        assertEquals("two words", setting.value());
    }
}
