package net.blockhost.anarchyclient.ui;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.math.Size;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BUTTON_SIZE;
import static net.blockhost.anarchyclient.ui.PanelWidgets.drawIcon;

final class IconButton extends Component implements LayoutDebugLabel {

    private final Supplier<String> icon;
    private final Supplier<Color> color;
    private final Runnable action;
    private final BooleanSupplier visible;

    IconButton(final Supplier<String> icon, final Supplier<Color> color, final Runnable action,
               final BooleanSupplier visible) {
        this.icon = icon;
        this.color = color;
        this.action = action;
        this.visible = visible;
        this.fixedSize(ICON_BUTTON_SIZE, ICON_BUTTON_SIZE);
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        if (!this.visible.getAsBoolean()) {
            return;
        }
        drawIcon(renderer, this.icon.get(), size.width() / 2F, size.height() / 2F, this.color.get());
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
        if (!this.visible.getAsBoolean() || event.button() != MouseButton.LEFT) {
            return false;
        }
        this.action.run();
        return true;
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(ICON_BUTTON_SIZE, ICON_BUTTON_SIZE);
    }

    @Override
    public String layoutDebugLabel() {
        return this.visible.getAsBoolean() ? this.icon.get() : "";
    }
}