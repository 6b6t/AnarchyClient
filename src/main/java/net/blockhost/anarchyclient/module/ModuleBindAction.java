package net.blockhost.anarchyclient.module;

import java.util.Locale;
import java.util.Optional;

public enum ModuleBindAction {
    TOGGLE,
    HOLD,
    SMART;

    /** Parses an action name, or returns empty for an unknown one so a command can reject it. */
    public static Optional<ModuleBindAction> tryParse(final String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(ModuleBindAction.valueOf(value.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

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
