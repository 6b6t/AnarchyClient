package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.DOCK_BUTTON;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.drawIcon;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.math.Size;

final class DockButton extends Component implements LayoutDebugLabel {

    private final String icon;
    private final String name;
    private final BooleanSupplier selected;
    private final Runnable action;
    private final BooleanSupplier activeDot;
    private final Anim hover = new Anim();
    private boolean hovered;

    DockButton(
            final String icon,
            final String name,
            final BooleanSupplier selected,
            final Runnable action,
            final BooleanSupplier activeDot) {
        this.icon = icon;
        this.name = name;
        this.selected = selected;
        this.action = action;
        this.activeDot = activeDot;
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        boolean isSelected = this.selected.getAsBoolean();
        float glow = this.hover.toward(this.hovered || isSelected ? 1F : 0F, 14F);
        if (glow > 0.01F) {
            int alpha = Math.round((isSelected ? 40F : 26F) * glow);
            renderer.optimizedFillRoundedRect(
                    1F, 1F, size.width() - 2F, size.height() - 2F, 9F, Color.fromRGBA(255, 255, 255, alpha));
        }
        Color iconColor = isSelected ? GlassTheme.accent() : (this.hovered ? TEXT : MUTED);
        drawIcon(renderer, this.icon, size.width() / 2F, size.height() / 2F, iconColor);
        if (this.activeDot.getAsBoolean()) {
            renderer.fillCircle(size.width() - 6F, 6F, 2F, GlassTheme.accent());
        }
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
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
        if (event.button() == MouseButton.LEFT) {
            this.action.run();
            return true;
        }
        return false;
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), DOCK_BUTTON);
    }

    @Override
    public String layoutDebugLabel() {
        return this.name.toLowerCase(Locale.ROOT);
    }
}
