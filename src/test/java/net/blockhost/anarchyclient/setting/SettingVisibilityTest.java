package net.blockhost.anarchyclient.setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingVisibilityTest {

    @Test
    void settingsAreVisibleByDefault() {
        BooleanSetting setting = BooleanSetting.from(BooleanSetting.builder()
                .id("visible")
                .name("Visible")
                .defaultValue(true)
                .build());

        assertTrue(setting.visible());
    }

    @Test
    void visibilityCanDependOnAnotherSetting() {
        BooleanSetting parent = BooleanSetting.from(BooleanSetting.builder()
                .id("parent")
                .name("Parent")
                .defaultValue(false)
                .build());
        NumberSetting child = NumberSetting.from(NumberSetting.builder()
                .id("child")
                .name("Child")
                .defaultValue(1.0)
                .min(0.0)
                .max(10.0)
                .step(1.0)
                .build());

        child.visibleWhen(parent::value);

        assertFalse(child.visible());
        parent.value(true);
        assertTrue(child.visible());
    }
}
