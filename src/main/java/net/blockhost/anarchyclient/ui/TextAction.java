package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelWidgets.drawFittedText;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textWidth;

import java.util.function.Supplier;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

final class TextAction extends Component implements LayoutDebugLabel {

    private final Supplier<String> text;
    private final Supplier<Color> color;
    private final Runnable action;

    TextAction(final Supplier<String> text, final Supplier<Color> color, final Runnable action) {
        this.text = text;
        this.color = color;
        this.action = action;
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        String value = this.text.get();
        if (value.isBlank()) {
            return;
        }
        drawFittedText(
                this,
                renderer,
                value,
                this.color.get(),
                size.width(),
                size.height() / 2F,
                size.width(),
                TextOrigin.Horizontal.VISUAL_RIGHT,
                TextOrigin.Vertical.LOGICAL_CENTER);
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
        if (event.button() != MouseButton.LEFT) {
            return false;
        }
        this.action.run();
        return true;
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        if (this.rivet() == null) {
            return Size.EMPTY;
        }
        String value = this.text.get();
        if (value.isBlank()) {
            return Size.EMPTY;
        }
        return new Size(
                Math.min(textWidth(this, value, this.color.get()), constraints.width()),
                this.rivet().backend().font().height());
    }

    @Override
    public String layoutDebugLabel() {
        return this.text.get();
    }
}
