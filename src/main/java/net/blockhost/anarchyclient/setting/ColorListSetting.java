package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ColorListSetting extends Setting<List<SettingColor>> implements TextValueSetting {

    private ColorListSetting(final ColorListSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), List.copyOf(spec.defaultValue()), spec.aliases());
        this.value(spec.defaultValue());
    }

    public static ImmutableColorListSettingSpec.IdBuildStage builder() {
        return ImmutableColorListSettingSpec.builder();
    }

    public static ColorListSetting from(final ColorListSettingSpec spec) {
        return new ColorListSetting(spec);
    }

    @Override
    public String valueString() {
        return String.join(",", this.value().stream().map(SettingColor::hex).toList());
    }

    @Override
    public void valueFromString(final String value) {
        if (value == null || value.isBlank()) {
            this.value(List.of());
            return;
        }
        List<SettingColor> colors = new ArrayList<>();
        for (String token : colorTokens(value)) {
            colors.add(SettingColor.parse(token));
        }
        this.value(colors);
    }

    /**
     * Splits a list of colors on semicolons, on spaces, and on commas before a {@code #}, so {@code #ff0000,
     * #00ff00} and {@code #ff0000,#00ff00 0,0,255} both work. Semicolons separate RGB colors written with spaces
     * ({@code 255, 0, 0; 0, 0, 255}), and a single such color works on its own.
     */
    static List<String> colorTokens(final String value) {
        String trimmed = value.trim();
        if (trimmed.contains(";")) {
            return split(trimmed, ";");
        }
        if (!trimmed.contains("#")) {
            try {
                SettingColor.parse(trimmed);
                return List.of(trimmed);
            } catch (IllegalArgumentException exception) {
                // Not a single color: split it as a list below.
            }
        }
        return split(trimmed, "\\s+|,\\s*(?=#)");
    }

    private static List<String> split(final String value, final String separator) {
        List<String> tokens = new ArrayList<>();
        for (String token : value.split(separator)) {
            String cleaned = token.trim();
            if (cleaned.endsWith(",")) {
                cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
            }
            if (!cleaned.isBlank()) {
                tokens.add(cleaned);
            }
        }
        return tokens;
    }

    @Override
    protected List<SettingColor> sanitize(final List<SettingColor> value) {
        if (value == null) {
            return this.defaultValue();
        }
        Set<SettingColor> unique = new LinkedHashSet<>();
        for (SettingColor color : value) {
            if (color != null) {
                unique.add(color);
            }
        }
        return List.copyOf(unique);
    }

    @Override
    public JsonElement toJson() {
        JsonArray array = new JsonArray();
        for (SettingColor color : this.value()) {
            array.add(color.hex());
        }
        return array;
    }

    @Override
    public void fromJson(final JsonElement element) {
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
        List<SettingColor> colors = new ArrayList<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (entry != null && entry.isJsonPrimitive()) {
                colors.add(SettingColor.parse(entry.getAsString()));
            }
        }
        this.value(colors);
    }
}
