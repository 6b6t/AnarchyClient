package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

abstract class AbstractStringListSetting extends Setting<List<String>> implements TextValueSetting {

    private final List<String> suggestions;

    protected AbstractStringListSetting(final String id, final String name, final String description,
                                        final List<String> defaultValue, final List<String> aliases,
                                        final List<String> suggestions) {
        super(id, name, description, List.copyOf(defaultValue), aliases);
        this.suggestions = suggestions.stream()
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted()
                .toList();
        this.value(defaultValue);
    }

    @Override
    public final List<String> suggestions() {
        return this.suggestions;
    }

    @Override
    public final String valueString() {
        return String.join(",", this.value());
    }

    @Override
    public final void valueFromString(final String value) {
        this.value(this.parse(value));
    }

    @Override
    protected final List<String> sanitize(final List<String> value) {
        if (value == null) {
            return this.defaultValue();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String entry : value) {
            String normalized = this.normalize(entry);
            if (!normalized.isBlank()) {
                unique.add(normalized);
            }
        }
        return List.copyOf(unique);
    }

    @Override
    public final JsonElement toJson() {
        JsonArray array = new JsonArray();
        for (String entry : this.value()) {
            array.add(entry);
        }
        return array;
    }

    @Override
    public final void fromJson(final JsonElement element) {
        if (element == null) {
            return;
        }
        if (element.isJsonPrimitive()) {
            this.valueFromString(element.getAsString());
            return;
        }
        if (!element.isJsonArray()) {
            return;
        }
        List<String> parsed = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry != null && entry.isJsonPrimitive()) {
                parsed.addAll(this.parse(entry.getAsString()));
            }
        }
        this.value(parsed);
    }

    protected String normalize(final String value) {
        return value == null ? "" : value.trim();
    }

    protected final String normalizeIdentifier(final String value) {
        String normalized = this.normalize(value).toLowerCase(Locale.ROOT);
        return normalized.contains(":") || normalized.isBlank() ? normalized : "minecraft:" + normalized;
    }

    private List<String> parse(final String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> parsed = new ArrayList<>();
        for (String token : value.split("[,;\\s]+")) {
            String normalized = this.normalize(token);
            if (!normalized.isBlank()) {
                parsed.add(normalized);
            }
        }
        return List.copyOf(parsed);
    }
}
