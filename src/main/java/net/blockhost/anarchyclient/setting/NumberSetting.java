package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public final class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    public NumberSetting(final String id, final String name, final double defaultValue, final double min, final double max, final double step) {
        super(id, name, defaultValue);
        this.min = min;
        this.max = max;
        this.step = step;
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
        return Math.max(this.min, Math.min(this.max, value));
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
