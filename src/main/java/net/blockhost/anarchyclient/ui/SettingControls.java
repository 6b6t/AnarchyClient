package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.Setting;

import java.math.BigDecimal;

final class SettingControls {

    private SettingControls() {
    }

    static String displayValue(final Setting<?> setting) {
        Object value = setting.value();
        if (value instanceof Double number) {
            return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
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
        if (setting instanceof SelectSetting select) {
            select.next();
            return true;
        }
        return false;
    }

    static void setNumberFromProgress(final NumberSetting setting, final double progress) {
        double clampedProgress = Math.max(0, Math.min(1, progress));
        double raw = setting.min() + (setting.max() - setting.min()) * clampedProgress;
        double step = setting.step();
        if (step > 0) {
            raw = setting.min() + Math.round((raw - setting.min()) / step) * step;
        }
        setting.value(raw);
    }
}
