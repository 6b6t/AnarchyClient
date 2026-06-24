package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class BooleanSetting extends Setting<Boolean> {

    private BooleanSetting(final BooleanSettingSpec spec) {
        super(spec.id(), spec.name(), spec.defaultValue(), spec.aliases());
    }

    public static ImmutableBooleanSettingSpec.IdBuildStage builder() {
        return ImmutableBooleanSettingSpec.builder();
    }

    public static BooleanSetting from(final BooleanSettingSpec spec) {
        return new BooleanSetting(spec);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.value());
    }

    @Override
    public void fromJson(final JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            this.value(element.getAsBoolean());
        }
    }
}
