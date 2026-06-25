package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public abstract class RegistryListSetting<T> extends Setting<List<T>> implements TextValueSetting {

    private final Function<Identifier, Optional<T>> resolver;
    private final Function<T, Identifier> keyGetter;
    private final List<String> suggestions;

    protected RegistryListSetting(final String id, final String name, final String description,
                                  final List<T> defaultValue, final List<String> aliases,
                                  final Function<Identifier, Optional<T>> resolver,
                                  final Function<T, Identifier> keyGetter,
                                  final List<Identifier> suggestions) {
        super(id, name, description, List.copyOf(defaultValue), aliases);
        this.resolver = resolver;
        this.keyGetter = keyGetter;
        this.suggestions = suggestions.stream().map(Identifier::toString).sorted().toList();
        this.value(defaultValue);
    }

    public final List<String> ids() {
        return this.value().stream().map(value -> this.keyGetter.apply(value).toString()).toList();
    }

    public final List<String> suggestions() {
        return this.suggestions;
    }

    public final String valueString() {
        return String.join(",", this.ids());
    }

    public final void valueFromString(final String value) {
        this.value(this.parse(value));
    }

    public final List<T> parse(final String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<T> parsed = new ArrayList<>();
        for (String token : value.split("[,;\\s]+")) {
            String normalized = token.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) {
                continue;
            }
            Identifier id = normalized.contains(":")
                    ? Identifier.tryParse(normalized)
                    : Identifier.withDefaultNamespace(normalized);
            if (id == null) {
                continue;
            }
            this.resolver.apply(id).ifPresent(parsed::add);
        }
        return List.copyOf(parsed);
    }

    @Override
    protected final List<T> sanitize(final List<T> value) {
        if (value == null) {
            return this.defaultValue();
        }
        Map<Identifier, T> unique = new LinkedHashMap<>();
        for (T entry : value) {
            if (entry != null) {
                unique.putIfAbsent(this.keyGetter.apply(entry), entry);
            }
        }
        return List.copyOf(unique.values());
    }

    @Override
    public final JsonElement toJson() {
        JsonArray array = new JsonArray();
        for (String id : this.ids()) {
            array.add(id);
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
        List<T> parsed = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry != null && entry.isJsonPrimitive()) {
                parsed.addAll(this.parse(entry.getAsString()));
            }
        }
        this.value(parsed);
    }
}
