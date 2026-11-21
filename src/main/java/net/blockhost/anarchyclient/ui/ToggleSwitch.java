package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_TRACK_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_WIDTH;
import static net.blockhost.anarchyclient.ui.PanelStyle.TRACK;

import java.util.function.BooleanSupplier;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.math.Size;

/**
 * Animated pill toggle. Owns its bounds and click handling so the painted switch and its
 * clickable area can never drift apart; the thumb glides between states.
 */
final class ToggleSwitch extends Component {

    private final BooleanSupplier visible;
    private final BooleanSupplier state;
    private final Runnable onToggle;
    private final Anim thumb = new Anim();

    ToggleSwitch(final BooleanSupplier state, final Runnable onToggle) {
        this(() -> true, state, onToggle);
    }

    ToggleSwitch(final BooleanSupplier visible, final BooleanSupplier state, final Runnable onToggle) {
        this.visible = visible;
        this.state = state;
        this.onToggle = onToggle;
        this.fixedSize(SWITCH_WIDTH, SWITCH_HEIGHT);
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        if (!this.visible.getAsBoolean()) {
            return;
        }
        boolean checked = this.state.getAsBoolean();
        float progress = this.thumb.toward(checked ? 1F : 0F, 16F);
        float trackY = (SWITCH_HEIGHT - SWITCH_TRACK_HEIGHT) / 2F;
        float radius = SWITCH_TRACK_HEIGHT / 2F;
        Color track = Color.interpolate(progress, TRACK, GlassTheme.accent());
        renderer.optimizedFillRoundedRect(0, trackY, SWITCH_WIDTH, SWITCH_TRACK_HEIGHT, radius, track);
        float thumbX = radius + (SWITCH_WIDTH - 2F * radius) * progress;
        renderer.fillCircle(thumbX, SWITCH_HEIGHT / 2F, radius - 2F, Color.fromRGB(250, 250, 250));
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
        if (this.visible.getAsBoolean() && event.button() == MouseButton.LEFT) {
            this.onToggle.run();
            return true;
        }
        return false;
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(SWITCH_WIDTH, SWITCH_HEIGHT);
    }
}
