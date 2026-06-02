package net.lenni0451.rivet.component.impl;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.ListenerList;
import net.lenni0451.rivet.input.keyboard.ModifierKey;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.input.mouse.MouseScrollEvent;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.theme.Theme;
import net.lenni0451.rivet.theme.ThemeOption;

import java.util.function.Consumer;

@Accessors(fluent = true, chain = true, makeFinal = true)
public class ColorPicker extends Component {

    private static final int HUE_STEPS = 12;

    @Getter
    private Color color;
    @Getter
    private final ListenerList<Consumer<Color>> colorChangeListener = new ListenerList<>();

    private float hue;
    private float saturation;
    private float brightness;
    private float alpha;

    private boolean draggingPicker;
    private boolean draggingHue;
    private boolean draggingAlpha;

    private final ThemeOption<Float> outlineWidth;
    private final ThemeOption<Color> outlineColor;
    private final ThemeOption<Float> pickerSize;
    private final ThemeOption<Float> sliderWidth;
    private final ThemeOption<Float> gap;
    private final ThemeOption<Float> selectorSize;

    public ColorPicker(final Color color) {
        this.color = color;
        this.updateHSB();

        this.outlineWidth = new ThemeOption<>(this, Theme.COLOR_PICKER_OUTLINE_WIDTH);
        this.outlineColor = new ThemeOption<>(this, Theme.COLOR_PICKER_OUTLINE_COLOR);
        this.pickerSize = new ThemeOption<>(this, Theme.COLOR_PICKER_PICKER_SIZE);
        this.sliderWidth = new ThemeOption<>(this, Theme.COLOR_PICKER_SLIDER_WIDTH);
        this.gap = new ThemeOption<>(this, Theme.COLOR_PICKER_GAP);
        this.selectorSize = new ThemeOption<>(this, Theme.COLOR_PICKER_SELECTOR_SIZE);
    }

    public ColorPicker color(final Color color) {
        this.color = color;
        this.updateHSB();
        return this;
    }

    private void updateHSB() {
        float[] hsb = this.color.toHSB();
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        this.alpha = this.color.getAlpha() / 255F;
    }

    private void updateColor() {
        this.color = Color.fromHSB(this.hue, this.saturation, this.brightness).withAlphaF(this.alpha);
        this.colorChangeListener.callVoid(c -> c.accept(this.color));
    }

    @Override
    protected void onComponentRemoved() {
        this.draggingPicker = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        if (!event.button().equals(MouseButton.LEFT)) return false;

        float pickerSize = this.pickerSize.value();
        float sliderWidth = this.sliderWidth.value();
        float gap = this.gap.value();

        if (event.x() >= 0 && event.x() <= pickerSize && event.y() >= 0 && event.y() <= pickerSize) {
            this.draggingPicker = true;
            this.updatePicker(event.x(), event.y());
            return true;
        } else if (event.x() >= pickerSize + gap && event.x() <= pickerSize + gap + sliderWidth && event.y() >= 0 && event.y() <= pickerSize) {
            this.draggingHue = true;
            this.updateHue(event.y());
            return true;
        } else if (event.x() >= 0 && event.x() <= pickerSize && event.y() >= pickerSize + gap && event.y() <= pickerSize + gap + sliderWidth) {
            this.draggingAlpha = true;
            this.updateAlpha(event.x());
            return true;
        }
        return false;
    }

    @Override
    protected boolean onComponentMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
        boolean consumed = this.draggingPicker || this.draggingHue || this.draggingAlpha;
        this.draggingPicker = false;
        this.draggingHue = false;
        this.draggingAlpha = false;
        return consumed;
    }

    @Override
    protected boolean onComponentMouseMove(final MouseMoveEvent event, final Rectangle bounds) {
        if (this.draggingPicker) {
            this.updatePicker(event.x(), event.y());
            return true;
        } else if (this.draggingHue) {
            this.updateHue(event.y());
            return true;
        } else if (this.draggingAlpha) {
            this.updateAlpha(event.x());
            return true;
        }
        return false;
    }

    @Override
    protected boolean onComponentMouseScroll(final MouseScrollEvent event, final Rectangle bounds) {
        if (this.rivet().backend().isKeyDown(ModifierKey.SHIFT)) {
            this.alpha = MathUtils.clamp(this.alpha + event.scrollY() / 20, 0, 1);
            this.updateColor();
            return true;
        } else if (this.rivet().backend().isKeyDown(ModifierKey.CONTROL)) {
            this.hue = MathUtils.clamp(this.hue - event.scrollY() / 20, 0, 1);
            this.updateColor();
            return true;
        }
        return false;
    }

    private void updatePicker(float mouseX, float mouseY) {
        float pickerSize = this.pickerSize.value();
        this.saturation = MathUtils.clamp(mouseX / pickerSize, 0, 1);
        this.brightness = MathUtils.clamp(1 - (mouseY / pickerSize), 0, 1);
        this.updateColor();
    }

    private void updateHue(float mouseY) {
        float pickerSize = this.pickerSize.value();
        this.hue = MathUtils.clamp(mouseY / pickerSize, 0, 1);
        this.updateColor();
    }

    private void updateAlpha(float mouseX) {
        float pickerSize = this.pickerSize.value();
        this.alpha = MathUtils.clamp(mouseX / pickerSize, 0, 1);
        this.updateColor();
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        float pickerSize = this.pickerSize.value();
        float sliderWidth = this.sliderWidth.value();
        float gap = this.gap.value();

        this.renderSaturationValue(renderer, pickerSize);
        this.renderHueSlider(renderer, pickerSize, sliderWidth, gap);
        this.renderAlphaSlider(renderer, pickerSize, sliderWidth, gap);
        this.renderPreview(renderer, pickerSize, sliderWidth, gap);
    }

    private void renderSaturationValue(final Renderer renderer, final float pickerSize) {
        renderer.fillRect(0, 0, pickerSize, pickerSize, Color.fromHSB(this.hue, 1F, 1F));
        renderer.fillGradientRect(0, 0, pickerSize, pickerSize, Color.WHITE, Color.WHITE, Color.WHITE.withAlpha(0), Color.WHITE.withAlpha(0));
        renderer.fillGradientRect(0, 0, pickerSize, pickerSize, Color.BLACK.withAlpha(0), Color.BLACK, Color.BLACK, Color.BLACK.withAlpha(0));

        // Selection circle
        float cursorX = this.saturation * pickerSize;
        float cursorY = (1 - this.brightness) * pickerSize;
        renderer.outlineRect(0, 0, pickerSize, pickerSize, this.outlineWidth.value(), this.outlineColor.value());
        renderer.outlineCircle(cursorX, cursorY, this.selectorSize.value(), 1, this.brightness > 0.5 ? Color.BLACK : Color.WHITE);
    }

    private void renderHueSlider(final Renderer renderer, final float pickerSize, final float sliderWidth, final float gap) {
        float x = pickerSize + gap;
        float stepHeight = pickerSize / HUE_STEPS;
        for (int i = 0; i < HUE_STEPS; i++) {
            float h1 = (float) i / HUE_STEPS;
            float h2 = (float) (i + 1) / HUE_STEPS;
            Color c1 = Color.fromHSB(h1, 1, 1);
            Color c2 = Color.fromHSB(h2, 1, 1);

            renderer.fillGradientRect(x, i * stepHeight, sliderWidth, stepHeight, c1, c2, c2, c1);
        }

        float cursorY = this.hue * pickerSize;
        renderer.outlineRect(x - 1, cursorY - 2, sliderWidth + 2, 4, 1, Color.WHITE);
        renderer.outlineRect(x, 0, sliderWidth, pickerSize, this.outlineWidth.value(), this.outlineColor.value());
    }

    private void renderAlphaSlider(final Renderer renderer, final float pickerSize, final float sliderWidth, final float gap) {
        float y = pickerSize + gap;
        // Background checkerboard
        float checkSize = sliderWidth / 2F;
        for (float cx = 0; cx < pickerSize; cx += checkSize) {
            for (float cy = 0; cy < sliderWidth; cy += checkSize) {
                boolean light = ((int) (cx / checkSize) + (int) (cy / checkSize)) % 2 == 0;
                renderer.fillRect(cx, y + cy, Math.min(checkSize, pickerSize - cx), Math.min(checkSize, sliderWidth - cy), light ? Color.WHITE : Color.LIGHT_GRAY);
            }
        }

        // Alpha gradient
        Color opaque = Color.fromHSB(this.hue, this.saturation, this.brightness);
        Color transparent = Color.fromHSB(this.hue, this.saturation, this.brightness).withAlpha(0);
        renderer.fillGradientRect(0, y, pickerSize, sliderWidth, transparent, transparent, opaque, opaque);

        float cursorX = this.alpha * pickerSize;
        renderer.outlineRect(cursorX - 2, y - 1, 4, sliderWidth + 2, 1, Color.WHITE);
        renderer.outlineRect(0, y, pickerSize, sliderWidth, this.outlineWidth.value(), this.outlineColor.value());
    }

    private void renderPreview(final Renderer renderer, final float pickerSize, final float sliderWidth, final float gap) {
        renderer.fillRect(pickerSize + gap, pickerSize + gap, sliderWidth / 2F, sliderWidth / 2F, Color.WHITE);
        renderer.fillRect(pickerSize + gap + sliderWidth / 2F, pickerSize + gap, sliderWidth / 2F, sliderWidth / 2F, Color.LIGHT_GRAY);
        renderer.fillRect(pickerSize + gap + sliderWidth / 2F, pickerSize + gap + sliderWidth / 2F, sliderWidth / 2F, sliderWidth / 2F, Color.WHITE);
        renderer.fillRect(pickerSize + gap, pickerSize + gap + sliderWidth / 2F, sliderWidth / 2F, sliderWidth / 2F, Color.LIGHT_GRAY);
        renderer.fillRect(pickerSize + gap, pickerSize + gap, sliderWidth, sliderWidth, this.color);
        renderer.outlineRect(pickerSize + gap, pickerSize + gap, sliderWidth, sliderWidth, this.outlineWidth.value(), this.outlineColor.value());
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        float pickerSize = this.pickerSize.value();
        float sliderWidth = this.sliderWidth.value();
        float gap = this.gap.value();
        return new Size(pickerSize + gap + sliderWidth, pickerSize + gap + sliderWidth);
    }

}
