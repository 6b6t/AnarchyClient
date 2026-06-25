package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class StringSetting extends Setting<String> implements TextValueSetting {

    private StringSetting(final StringSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases());
    }

    public static ImmutableStringSettingSpec.IdBuildStage builder() {
        return ImmutableStringSettingSpec.builder();
    }

    public static StringSetting from(final StringSettingSpec spec) {
        return new StringSetting(spec);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.value());
    }

    @Override
    public String valueString() {
        return this.value();
    }

    @Override
    public void valueFromString(final String value) {
        this.value(value);
    }

    @Override
    public void fromJson(final JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            this.value(element.getAsString());
        }
    }
}
