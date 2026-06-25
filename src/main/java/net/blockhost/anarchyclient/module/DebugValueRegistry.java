package net.blockhost.anarchyclient.module;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class DebugValueRegistry {

    private static final Map<String, LinkedHashMap<String, String>> VALUES = new LinkedHashMap<>();

    private DebugValueRegistry() {
    }

    public static synchronized void put(final String owner, final String key, final Object value) {
        if (owner == null || owner.isBlank() || key == null || key.isBlank()) {
            return;
        }
        VALUES.computeIfAbsent(owner, ignored -> new LinkedHashMap<>()).put(key, String.valueOf(value));
    }

    public static synchronized void remove(final String owner, final String key) {
        LinkedHashMap<String, String> values = VALUES.get(owner);
        if (values == null) {
            return;
        }
        values.remove(key);
        if (values.isEmpty()) {
            VALUES.remove(owner);
        }
    }

    public static synchronized void clear(final String owner) {
        if (owner != null) {
            VALUES.remove(owner);
        }
    }

    public static synchronized void clearAll() {
        VALUES.clear();
    }

    public static synchronized Optional<String> value(final String owner, final String key) {
        LinkedHashMap<String, String> values = VALUES.get(owner);
        return values == null ? Optional.empty() : Optional.ofNullable(values.get(key));
    }

    public static synchronized Map<String, Map<String, String>> snapshot() {
        LinkedHashMap<String, Map<String, String>> copy = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<String, String>> entry : VALUES.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }
}
