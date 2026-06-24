package net.blockhost.anarchyclient.module;

import java.util.Locale;

public enum ModuleBindAction {
    TOGGLE,
    HOLD,
    SMART;

    public static ModuleBindAction parse(final String value) {
        if (value == null || value.isBlank()) {
            return TOGGLE;
        }
        try {
            return ModuleBindAction.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return TOGGLE;
        }
    }
}
