package net.blockhost.anarchyclient.setting;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

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

    @Test
    void replacesNonFiniteValuesWithDefault() {
        NumberSetting setting = rangeSetting();

        setting.value(8.0);
        setting.value(Double.NaN);
        assertEquals(5.0, setting.value());

        setting.value(8.0);
        setting.fromJson(new JsonPrimitive("NaN"));
        assertEquals(5.0, setting.value());

        setting.value(Double.POSITIVE_INFINITY);
        assertEquals(5.0, setting.value());
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

    @Test
    void snapsSliderValuesToTheGridFromTheMinimumWithoutFloatingPointNoise() {
        NumberSetting setting = NumberSetting.from(NumberSetting.builder()
                .id("height")
                .name("Height")
                .defaultValue(0.42)
                .min(0.42)
                .max(2.0)
                .step(0.05)
                .build());
        NumberSetting tenths = NumberSetting.from(NumberSetting.builder()
                .id("speed")
                .name("Speed")
                .defaultValue(0.1)
                .min(0.0)
                .max(1.0)
                .step(0.1)
                .build());

        assertEquals(0.47, setting.snap(0.48));
        assertEquals(0.3, tenths.snap(0.1 + 0.2));
        assertEquals(1.0, tenths.snap(5.0));
    }
}
