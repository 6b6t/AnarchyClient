package net.lenni0451.rivet.component.impl;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.ListenerList;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.theme.Theme;
import net.lenni0451.rivet.theme.ThemeOption;

import java.util.function.Consumer;

@Accessors(fluent = true, chain = true, makeFinal = true)
public class Checkbox extends Component {

    @Getter
    private boolean checked;
    @Getter
    private final ListenerList<Consumer<Boolean>> toggleListener = new ListenerList<>();
    @Getter
    private String text;
    private ShapedText shapedText;
    private boolean hovered = false;

    @Getter
    private final ThemeOption<Float> cornerRadius;
    @Getter
    private final ThemeOption<Float> outlineWidth;
    @Getter
    private final ThemeOption<Color> backgroundColor;
    @Getter
    private final ThemeOption<Color> outlineColor;
    @Getter
    private final ThemeOption<Color> checkColor;
    @Getter
    private final ThemeOption<Float> checkWidth;
    @Getter
    private final ThemeOption<Float> textGap;

    public Checkbox() {
        this(false);
    }

    public Checkbox(final boolean checked) {
        this("", checked);
    }

    public Checkbox(final String text, final boolean checked) {
        this.checked = checked;
        this.text = text;

        this.cornerRadius = new ThemeOption<>(this, Theme.CHECKBOX_CORNER_RADIUS);
        this.outlineWidth = new ThemeOption<>(this, Theme.CHECKBOX_OUTLINE_WIDTH);
        this.backgroundColor = new ThemeOption<>(this, Theme.CHECKBOX_BACKGROUND_COLOR);
        this.outlineColor = new ThemeOption<>(this, Theme.CHECKBOX_OUTLINE_COLOR);
        this.checkColor = new ThemeOption<>(this, Theme.CHECKBOX_CHECK_COLOR);
        this.checkWidth = new ThemeOption<>(this, Theme.CHECKBOX_CHECK_WIDTH);
        this.textGap = new ThemeOption<>(this, Theme.CHECKBOX_TEXT_GAP);
    }

    public Checkbox checked(final boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            this.toggleListener.callVoid(c -> c.accept(this.checked));
        }
        return this;
    }

    public Checkbox text(final String text) {
        if (!this.text.equals(text)) {
            this.text = text;
            if (this.rivet() != null) {
                this.shapeText();
                this.parent().requestLayoutRecalculation();
            }
        }
        return this;
    }

    private void shapeText() {
        this.shapedText = this.rivet().backend().shapeText(this.text, this.rivet().theme().get(Theme.TEXT_COLOR));
    }

    @Override
    protected void onComponentAdded() {
        this.shapeText();
    }

    @Override
    protected void onComponentRemoved() {
        this.hovered = false;
    }

    @Override
    protected void onComponentMouseEnter() {
        this.hovered = true;
    }

    @Override
    protected void onComponentMouseLeave() {
        this.hovered = false;
    }

    @Override
    protected boolean onComponentMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
        if (this.hovered && event.button().equals(MouseButton.LEFT)) {
            this.checked(!this.checked);
            return true;
        }
        return false;
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        float boxSize = bounds.height() * 0.8F;
        float offset = (bounds.height() - boxSize) / 2F;

        renderer.optimizedFillRoundedRect(offset, offset, boxSize, boxSize, this.cornerRadius.value(), this.backgroundColor.value());
        if (this.outlineWidth.value() > 0) {
            renderer.optimizedOutlineRoundedRect(offset, offset, boxSize, boxSize, this.cornerRadius.value(), this.outlineWidth.value(), this.outlineColor.value());
        }

        if (this.checked) {
            float padding = boxSize * 0.2F;
            float checkWidth = this.checkWidth.value();
            Color checkColor = this.checkColor.value();

            renderer.line(offset + padding, offset + boxSize / 2F, offset + boxSize / 2.5F, offset + boxSize - padding, checkWidth, checkColor);
            renderer.line(offset + boxSize / 2.5F, offset + boxSize - padding, offset + boxSize - padding, offset + padding, checkWidth, checkColor);
        }

        if (!this.text.isEmpty()) {
            renderer.text(this.shapedText, offset + bounds.height() + this.textGap.value(), bounds.height() / 2F, TextOrigin.Horizontal.VISUAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
        }
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        float height = this.shapedText.logicalBounds().height();
        float width = height;
        if (!this.text.isEmpty()) {
            width += this.textGap.value() + this.shapedText.visualBounds().width();
        }
        return new Size(width, height);
    }

}
