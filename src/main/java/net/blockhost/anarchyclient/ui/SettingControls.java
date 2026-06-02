package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.Setting;

final class SettingControls {

    private SettingControls() {
    }

    static String displayValue(final Setting<?> setting) {
        Object value = setting.value();
        if (value instanceof Double number) {
            return number % 1 == 0 ? Integer.toString(number.intValue()) : String.format("%.1f", number);
        }
        return String.valueOf(value);
    }

    static boolean adjust(final Setting<?> setting) {
        if (setting instanceof BooleanSetting bool) {
            bool.value(!bool.value());
            return true;
        }
        if (setting instanceof NumberSetting number) {
            double next = number.value() + number.step();
            if (next > number.max()) {
                next = number.min();
            }
            number.value(next);
            return true;
        }
        return false;
    }
}
