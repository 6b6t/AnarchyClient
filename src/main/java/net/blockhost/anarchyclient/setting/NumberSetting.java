package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.lenni0451.commons.math.MathUtils;

public final class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    private NumberSetting(final NumberSettingSpec spec) {
        super(spec.id(), spec.name(), spec.defaultValue());
        this.min = spec.min();
        this.max = spec.max();
        this.step = spec.step();
    }

    public static ImmutableNumberSettingSpec.IdBuildStage builder() {
        return ImmutableNumberSettingSpec.builder();
    }

    public static NumberSetting from(final NumberSettingSpec spec) {
        return new NumberSetting(spec);
    }

    public double min() {
        return this.min;
    }

    public double max() {
        return this.max;
    }

    public double step() {
        return this.step;
    }

    @Override
    protected Double sanitize(final Double value) {
        return MathUtils.clamp(value, this.min, this.max);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.value());
    }

    @Override
    public void fromJson(final JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            this.value(element.getAsDouble());
        }
    }
}
