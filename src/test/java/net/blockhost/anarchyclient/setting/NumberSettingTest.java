package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberSettingTest {

    @Test
    void clampsAssignedValuesToRange() {
        NumberSetting setting = rangeSetting();

        setting.value(99.0);
        assertEquals(10.0, setting.value());

        setting.value(-4.0);
        assertEquals(1.0, setting.value());
    }

    @Test
    void clampsJsonValuesToRange() {
        NumberSetting setting = rangeSetting();

        setting.fromJson(new JsonPrimitive(14.0));

        assertEquals(10.0, setting.value());
    }

    private static NumberSetting rangeSetting() {
        return NumberSetting.from(NumberSetting.builder()
                .id("range")
                .name("Range")
                .defaultValue(5.0)
                .min(1.0)
                .max(10.0)
                .step(0.5)
                .build());
    }
}
