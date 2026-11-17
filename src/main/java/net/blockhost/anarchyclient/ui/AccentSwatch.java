package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.math.Size;

import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;

final class AccentSwatch extends Component implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final ClientConfig.GuiThemePreset preset;
    private final Anim hover = new Anim();
    private boolean hovered;

    AccentSwatch(final ModulePanel panel, final ClientConfig.GuiThemePreset preset) {
        this.panel = panel;
        this.preset = preset;
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        float centerX = size.width() / 2F;
        float centerY = size.height() / 2F;
        float radius = 8F + this.hover.toward(this.hovered ? 1F : 0F, 14F);
        if (this.preset == GlassTheme.preset()) {
            // Selection ring as a larger filled circle behind the swatch: both circles render
            // through the anti-aliased SDF path, unlike a tessellated outline.
            renderer.fillCircle(centerX, centerY, radius + 2F, TEXT);
        }
        renderer.fillCircle(centerX, centerY, radius, GuiThemePalette.of(this.preset).active());
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
            this.panel.selectThemePreset(this.preset);
            return true;
        }
        return false;
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(AccentRow.SWATCH_BOX, AccentRow.SWATCH_BOX);
    }

    @Override
    public String layoutDebugLabel() {
        return this.preset.key();
    }
}