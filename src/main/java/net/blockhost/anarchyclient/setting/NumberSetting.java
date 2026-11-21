package net.blockhost.anarchyclient.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.math.BigDecimal;
import java.math.RoundingMode;
import net.lenni0451.commons.math.MathUtils;
import org.jspecify.annotations.Nullable;

public final class NumberSetting extends Setting<Double> {

    private final double min;
    private final double max;
    private final double step;

    private NumberSetting(final NumberSettingSpec spec) {
        super(spec.id(), spec.name(), spec.description(), spec.defaultValue(), spec.aliases());
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

    /**
     * Snaps a slider value to the step grid starting at {@link #min()}, rounded to the step's precision. Rivet snaps
     * from 0 in floating point, which stored values such as 0.30000000000000004 and left ranges like 0.42 to 2.0
     * (step 0.05) off their own grid. Not applied in {@link #sanitize}: config and commands may set any value.
     */
    public double snap(final double value) {
        if (!Double.isFinite(value) || this.step <= 0.0) {
            return MathUtils.clamp(value, this.min, this.max);
        }
        double snapped = this.min + Math.round((value - this.min) / this.step) * this.step;
        int scale = Math.max(
                BigDecimal.valueOf(this.step).stripTrailingZeros().scale(),
                BigDecimal.valueOf(this.min).stripTrailingZeros().scale());
        double rounded = BigDecimal.valueOf(snapped)
                .setScale(Math.max(0, scale), RoundingMode.HALF_UP)
                .doubleValue();
        return MathUtils.clamp(rounded, this.min, this.max);
    }

    @Override
    protected Double sanitize(final Double value) {
        // NaN slips through clamp, and Gson refuses to save non-finite numbers.
        if (value == null || !Double.isFinite(value)) {
            return this.defaultValue();
        }
        return MathUtils.clamp(value, this.min, this.max);
    }

    @Override
    public JsonElement toJson() {
        return new JsonPrimitive(this.value());
    }

    @Override
    public void fromJson(final @Nullable JsonElement element) {
        if (element != null && element.isJsonPrimitive()) {
            this.value(element.getAsDouble());
        }
    }
}
