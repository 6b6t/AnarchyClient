package net.blockhost.anarchyclient.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.blockhost.anarchyclient.setting.NumberSetting;
import org.junit.jupiter.api.Test;

class NumberSettingRowTest {

    @Test
    void theSliderCountsStepsFromTheMinimum() {
        NumberSetting reach = NumberSetting.from(NumberSetting.builder()
                .id("reach")
                .name("Reach")
                .defaultValue(4.5)
                .min(3.0)
                .max(6.0)
                .step(0.5)
                .build());
        NumberSetting height = NumberSetting.from(NumberSetting.builder()
                .id("height")
                .name("Height")
                .defaultValue(0.42)
                .min(0.42)
                .max(2.0)
                .step(0.05)
                .build());

        assertEquals(6, NumberSettingRow.stepCount(reach));
        assertEquals(3.0, NumberSettingRow.stepIndex(reach, 4.5));
        // 0.42 to 2.0 is 31.6 steps: the last index reaches the maximum.
        assertEquals(32, NumberSettingRow.stepCount(height));
        assertEquals(0.47, height.snap(height.min() + 1 * NumberSettingRow.stepSize(height)));
        assertEquals(2.0, height.snap(height.min() + 32 * NumberSettingRow.stepSize(height)));
    }
}
