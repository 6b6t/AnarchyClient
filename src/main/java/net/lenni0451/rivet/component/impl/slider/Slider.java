package net.lenni0451.rivet.component.impl.slider;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.ListenerList;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.theme.Theme;
import net.lenni0451.rivet.theme.ThemeOption;
import net.lenni0451.rivet.utils.FormatUtils;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Accessors(fluent = true, chain = true, makeFinal = true)
public class Slider extends Component {

    private static final int TICK_OFFSET = 2;

    @Getter
    private double min;
    @Getter
    private double max;
    @Getter
    private double step;
    @Getter
    @Setter
    private double value;
    @Getter
    private final ListenerList<Consumer<Double>> valueChangeListener = new ListenerList<>();
    @Getter
    @Nullable
    private Ticks ticks;
    private boolean dragged;
    private SliderTooltip tooltip;
    private final Map<Double, ShapedText> tickLabels = new HashMap<>();
    private String cachedFormatString = null;

    @Getter
    private final ThemeOption<Color> barColor;
    @Getter
    private final ThemeOption<Color> activeBarColor;
    @Getter
    private final ThemeOption<Color> thumbColor;
    @Getter
    private final ThemeOption<Color> thumbClickColor;
    @Getter
    private final ThemeOption<Color> tickColor;
    @Getter
    private final ThemeOption<Float> barHeight;
    @Getter
    private final ThemeOption<Float> thumbWidth;
    @Getter
    private final ThemeOption<Float> thumbHeight;
    @Getter
    private final ThemeOption<Float> barCornerRadius;
    @Getter
    private final ThemeOption<Float> thumbCornerRadius;
    @Getter
    private final ThemeOption<Boolean> thumbEncased;
    @Getter
    private final ThemeOption<ThumbShape> thumbShape;
    @Getter
    private final ThemeOption<String> tooltipFormat;

    public Slider(final double min, final double max, final double value) {
        this(min, max, 1, value);
    }

    public Slider(final double min, final double max, final double step, final double value) {
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = value;

        this.barColor = new ThemeOption<>(this, Theme.SLIDER_BAR_COLOR);
        this.activeBarColor = new ThemeOption<>(this, Theme.SLIDER_ACTIVE_BAR_COLOR);
        this.thumbColor = new ThemeOption<>(this, Theme.SLIDER_THUMB_COLOR);
        this.thumbClickColor = new ThemeOption<>(this, Theme.SLIDER_THUMB_CLICK_COLOR);
        this.tickColor = new ThemeOption<>(this, Theme.SLIDER_TICK_COLOR);
        this.barHeight = new ThemeOption<>(this, Theme.SLIDER_BAR_HEIGHT);
        this.thumbWidth = new ThemeOption<>(this, Theme.SLIDER_THUMB_WIDTH);
        this.thumbHeight = new ThemeOption<>(this, Theme.SLIDER_THUMB_HEIGHT);
        this.barCornerRadius = new ThemeOption<>(this, Theme.SLIDER_BAR_CORNER_RADIUS);
        this.thumbShape = new ThemeOption<>(this, Theme.SLIDER_THUMB_SHAPE);
        this.thumbCornerRadius = new ThemeOption<>(this, Theme.SLIDER_THUMB_CORNER_RADIUS);
        this.thumbEncased = new ThemeOption<>(this, Theme.SLIDER_THUMB_ENCASED);
        this.tooltipFormat = new ThemeOption<>(this, Theme.SLIDER_TOOLTIP_FORMAT);
        this.tooltipFormat.changeListener().add(f -> this.cachedFormatString = null);
    }

    public Slider min(final double min) {
        this.min = min;
        this.tickLabels.clear();
        return this;
    }

    public Slider max(final double max) {
        this.max = max;
        this.tickLabels.clear();
        return this;
    }

    public Slider step(final double step) {
        this.step = step;
        this.cachedFormatString = null;
        return this;
    }

    public Slider ticks(@Nullable final Ticks ticks) {
        this.ticks = ticks;
        this.tickLabels.clear();
        return this;
    }

    @Override
    protected void onComponentRemoved() {
        if (this.tooltip != null) {
            this.tooltip.remove();
            this.tooltip = null;
        }
        this.dragged = false;
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        if (event.button().equals(MouseButton.LEFT)) {
            this.dragged = true;
            this.tooltip = new SliderTooltip(this.formatValue(this.value));
            this.tooltip.add(this.rivet());
            this.updateValue(event.x(), bounds);
            return true;
        }
        return false;
    }

    @Override
    protected boolean onComponentMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
        if (event.button().equals(MouseButton.LEFT)) {
            this.dragged = false;
            this.tooltip.remove();
            this.tooltip = null;
            return true;
        }
        return false;
    }

    @Override
    protected boolean onComponentMouseMove(final MouseMoveEvent event, final Rectangle bounds) {
        if (this.dragged) {
            this.updateValue(event.x(), bounds);
            return true;
        }
        return false;
    }

    private void updateValue(final float mouseX, final Rectangle bounds) {
        float thumbWidth = this.thumbWidth.value();
        float barWidth = this.barWidth(bounds);
        float progress = (mouseX - thumbWidth / 2F) / barWidth;
        progress = MathUtils.clamp(progress, 0, 1);
        double newValue = this.min + progress * (this.max - this.min);
        newValue = Math.round(newValue / this.step) * this.step;
        newValue = MathUtils.clamp(newValue, this.min, this.max);
        if (this.value != newValue) {
            this.value = newValue;
            if (this.tooltip != null) {
                this.tooltip.text(this.formatValue(this.value));
            }
            this.valueChangeListener.callVoid(c -> c.accept(this.value));
        }
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        float thumbWidth = this.thumbWidth.value();
        float thumbHeight = this.thumbHeight.value();
        float barHeight = this.barHeight.value();
        float sliderCenter = this.ticks != null ? thumbHeight / 2F : bounds.height() / 2F;
        float barWidth = this.barWidth(bounds);
        double progress = (this.value - this.min) / (this.max - this.min);
        float thumbX = (float) (thumbWidth / 2F + barWidth * progress);

        this.renderBar(renderer, bounds, sliderCenter, barHeight, thumbWidth, thumbX);
        this.renderThumb(renderer, sliderCenter, thumbWidth, thumbHeight, thumbX);
        if (this.ticks != null) this.renderTicks(renderer, sliderCenter, barHeight, thumbWidth, thumbHeight, barWidth);
        if (this.tooltip != null) {
            this.tooltip.position(bounds.x() + thumbX, bounds.y(), bounds.height());
        }
    }

    private String formatValue(final double value) {
        if (this.cachedFormatString == null) {
            this.cachedFormatString = FormatUtils.formatDecimalString(this.tooltipFormat.value(), this.step);
        }
        return String.format(this.cachedFormatString, value);
    }

    private void renderBar(final Renderer renderer, final Rectangle bounds, final float sliderCenter, final float barHeight, final float thumbWidth, final float thumbX) {
        if (this.thumbEncased.value()) {
            renderer.optimizedFillRoundedRect(0, sliderCenter - barHeight / 2F, bounds.width(), barHeight, this.barCornerRadius.value(), this.barColor.value());
            renderer.optimizedFillRoundedRect(0, sliderCenter - barHeight / 2F, thumbX, barHeight, this.barCornerRadius.value(), this.activeBarColor.value());
        } else {
            renderer.optimizedFillRoundedRect(thumbWidth / 2F, sliderCenter - barHeight / 2F, bounds.width() - thumbWidth, barHeight, this.barCornerRadius.value(), this.barColor.value());
            renderer.optimizedFillRoundedRect(thumbWidth / 2F, sliderCenter - barHeight / 2F, thumbX - thumbWidth / 2F, barHeight, this.barCornerRadius.value(), this.activeBarColor.value());
        }
    }

    private void renderThumb(final Renderer renderer, final float sliderCenter, final float thumbWidth, final float thumbHeight, final float thumbX) {
        Color color = this.dragged ? this.thumbClickColor.value() : this.thumbColor.value();
        float cornerRadius = this.thumbCornerRadius.value();
        switch (this.thumbShape.value()) {
            case CIRCLE -> renderer.fillCircle(thumbX, sliderCenter, Math.min(thumbWidth, thumbHeight) / 2F, color);
            case SQUARE -> {
                float size = Math.min(thumbWidth, thumbHeight);
                renderer.optimizedFillRoundedRect(thumbX - size / 2F, sliderCenter - size / 2F, size, size, cornerRadius, color);
            }
            case RECTANGLE -> renderer.optimizedFillRoundedRect(thumbX - thumbWidth / 2F, sliderCenter - thumbHeight / 2F, thumbWidth, thumbHeight, cornerRadius, color);
            case PIN -> {
                renderer.fillRect(thumbX - thumbWidth / 2F, sliderCenter - thumbHeight / 2F, thumbWidth, thumbHeight / 2F, color);
                renderer.fillTriangle(thumbX - thumbWidth / 2F, sliderCenter, thumbX, sliderCenter + thumbHeight / 2F, thumbX + thumbWidth / 2F, sliderCenter, color);
            }
        }
    }

    private void renderTicks(final Renderer renderer, final float sliderCenter, final float barHeight, final float thumbWidth, final float thumbHeight, final float barWidth) {
        float tickStartY = sliderCenter + thumbHeight / 2F + TICK_OFFSET;
        float majorTickLength = barHeight;
        float minorTickLength = barHeight / 2F;
        Color color = this.tickColor.value();

        if (this.ticks.majorTickSpacing > 0) {
            for (double tick = 0; ; tick += this.ticks.majorTickSpacing) {
                boolean lastTick = false;
                if (tick >= this.max - this.min) {
                    tick = this.max - this.min;
                    lastTick = true;
                }
                float tickX = (float) (thumbWidth / 2F + barWidth * (tick / (this.max - this.min)));
                renderer.fillRect(tickX - 1, tickStartY, TICK_OFFSET, majorTickLength, color);

                double tickValue = this.min + tick;
                ShapedText text = this.tickLabels.computeIfAbsent(tickValue, v -> this.rivet().backend().shapeText(this.ticks.labelProvider.getLabel(v), this.rivet().theme().get(Theme.TEXT_COLOR)));
                renderer.translate(tickX, tickStartY + majorTickLength + 2, () -> {
                    renderer.scale(0.5F, () -> {
                        renderer.text(text, 0, 0, TextOrigin.Horizontal.VISUAL_CENTER, TextOrigin.Vertical.LOGICAL_TOP);
                    });
                });
                if (lastTick) break;
            }
        }
        if (this.ticks.minorTickSpacing > 0) {
            for (double tick = 0; tick < this.max - this.min; tick += this.ticks.minorTickSpacing) {
                if (this.ticks.majorTickSpacing > 0 && Math.abs(tick % this.ticks.majorTickSpacing) < 1e-6) {
                    continue;
                }
                float tickX = (float) (thumbWidth / 2F + barWidth * (tick / (this.max - this.min)));
                renderer.fillRect(tickX, tickStartY, 1, minorTickLength, color);
            }
        }
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        float height;
        if (this.ticks == null) {
            height = this.thumbHeight.value();
        } else {
            height = this.thumbHeight.value() + TICK_OFFSET + this.barHeight.value() + TICK_OFFSET + this.rivet().backend().getTextHeight() / 2F;
        }
        return new Size(this.rivet().backend().getTextHeight() * 10, height);
    }

    private float barWidth(final Rectangle bounds) {
        return bounds.width() - this.thumbWidth.value();
    }


    public record Ticks(double majorTickSpacing, double minorTickSpacing, TickLabelProvider labelProvider) {
        public Ticks(final double majorTickSpacing, final double minorTickSpacing) {
            this(majorTickSpacing, minorTickSpacing, defaultLabelProvider(majorTickSpacing));
        }

        private static TickLabelProvider defaultLabelProvider(final double majorTickSpacing) {
            String format = FormatUtils.formatDecimalString("%,f", majorTickSpacing);
            return d -> String.format(format, d);
        }
    }

    @FunctionalInterface
    public interface TickLabelProvider {
        String getLabel(final double value);
    }

    public enum ThumbShape {
        CIRCLE,
        SQUARE,
        RECTANGLE,
        PIN
    }

}
