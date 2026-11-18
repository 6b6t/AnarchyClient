package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.TextValueSetting;

import java.math.BigDecimal;

final class SettingControls {

    private SettingControls() {
    }

    static String displayValue(final Setting<?> setting) {
        Object value = setting.value();
        if (value instanceof Double number) {
            return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
        }
        if (setting instanceof SelectSetting select) {
            return select.optionLabel(select.value());
        }
        if (setting instanceof TextValueSetting text) {
            return text.valueString();
        }
        return String.valueOf(value);
    }
}
