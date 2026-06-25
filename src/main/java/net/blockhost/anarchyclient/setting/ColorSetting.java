package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class ColorSetting extends Setting<SettingColor> implements TextValueSetting {

    private ColorSetting(final ColorSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases());
    }

    public static ImmutableColorSettingSpec.IdBuildStage builder() {
        return ImmutableColorSettingSpec.builder();
    }

    public static ColorSetting from(final ColorSettingSpec spec) {
        return new ColorSetting(spec);
    }

    @Override
    public String valueString() {
        return this.value().hex();
    }

    @Override
    public void valueFromString(final String value) {
        this.value(SettingColor.parse(value));
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.valueString());
    }

    @Override
    public void fromJson(final JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            this.valueFromString(element.getAsString());
        }
    }
}
