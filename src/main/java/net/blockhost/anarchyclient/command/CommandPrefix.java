package net.blockhost.anarchyclient.command;

import java.util.List;

/**
 * The single-character prefix that turns a chat line into a client command (Meteor-style), e.g. {@code
 * .toggle killaura}. It must stay one character so it lines up 1:1 with {@code /} — that is what lets the
 * vanilla chat suggestion machinery treat a prefixed line as a real command and show live tab-fill.
 *
 * <p>Runtime source of truth (like {@code GlassTheme}); persisted through {@code ClientConfig} and edited
 * from the Theme tab or the {@code prefix} command.</p>
 */
public final class CommandPrefix {

    /** Prefixes offered by the GUI cycle button; the command accepts any single character. */
    private static final List<String> COMMON = List.of(".", ",", ";", ":", "!", "-", ">", "+", "~");

    private static volatile String prefix = ".";

    private CommandPrefix() {
    }

    public static String get() {
        return prefix;
    }

    public static char first() {
        return prefix.charAt(0);
    }

    public static void set(final String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        prefix = value.trim().substring(0, 1);
    }

    /** Advance to the next common prefix (wraps). Used by the Theme tab button. */
    public static void cycle() {
        int index = COMMON.indexOf(prefix);
        prefix = COMMON.get((index + 1) % COMMON.size());
    }
}
