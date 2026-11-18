package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.blockhost.anarchyclient.i18n.Translations;

import java.util.List;

public final class SelectSetting extends Setting<String> {

    private final List<String> options;

    private SelectSetting(final SelectSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases());
        if (spec.options().isEmpty()) {
            throw new IllegalArgumentException("Select setting requires at least one option: " + spec.id());
        }
        if (!spec.options().contains(spec.defaultValue())) {
            throw new IllegalArgumentException("Default value must be an allowed option: " + spec.id());
        }
        this.options = List.copyOf(spec.options());
    }

    public static ImmutableSelectSettingSpec.IdBuildStage builder() {
        return ImmutableSelectSettingSpec.builder();
    }

    public static SelectSetting from(final SelectSettingSpec spec) {
        return new SelectSetting(spec);
    }

    public List<String> options() {
        return this.options;
    }

    /**
     * The label shown for {@code option} in the selected language. The stored value stays the English option, so
     * configs and commands do not depend on the language.
     */
    public String optionLabel(final String option) {
        String key = this.translationKey();
        return key == null ? option : Translations.text(optionKey(key, option), option);
    }

    public static String optionKey(final String settingKey, final String option) {
        return settingKey + ".option." + Translations.keySegment(option);
    }

    public void next() {
        int index = this.options.indexOf(this.value());
        this.value(this.options.get((index + 1) % this.options.size()));
    }

    @Override
    protected String sanitize(final String value) {
        return this.options.contains(value) ? value : this.defaultValue();
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.value());
    }

    @Override
    public void fromJson(final JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            this.value(element.getAsString());
        }
    }
}
