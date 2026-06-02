package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SelectSettingTest {

    @Test
    void cyclesThroughOptions() {
        SelectSetting setting = modeSetting();

        setting.next();
        assertEquals("Random", setting.value());

        setting.next();
        assertEquals("Sequential", setting.value());
    }

    @Test
    void fallsBackToDefaultForInvalidJsonValues() {
        SelectSetting setting = modeSetting();
        setting.value("Random");

        setting.fromJson(new JsonPrimitive("Missing"));

        assertEquals("Sequential", setting.value());
    }

    @Test
    void rejectsDefaultValuesOutsideOptions() {
        assertThrows(IllegalArgumentException.class, () -> SelectSetting.from(SelectSetting.builder()
                .id("mode")
                .name("Mode")
                .defaultValue("Missing")
                .addAllOptions(List.of("Sequential", "Random"))
                .build()));
    }

    private static SelectSetting modeSetting() {
        return SelectSetting.from(SelectSetting.builder()
                .id("mode")
                .name("Mode")
                .defaultValue("Sequential")
                .addAllOptions(List.of("Sequential", "Random"))
                .build());
    }
}
