package net.blockhost.anarchyclient.ui;

import java.util.function.Supplier;
import net.blockhost.anarchyclient.rivet.BackgroundDesign;
import net.blockhost.anarchyclient.rivet.GlassPanelCommand;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Size;

/**
 * A liquid glass panel background: refracts the blurred game scene captured by GlassBackdrop.
 * Fills its whole component bounds; layer it under content with a spanning grid cell.
 */
final class GlassSurface extends Component {

    private final Supplier<Color> tint;
    private final Supplier<Float> cornerRadius;
    private final Supplier<BackgroundDesign> design;

    GlassSurface(
            final Supplier<Color> tint, final Supplier<Float> cornerRadius, final Supplier<BackgroundDesign> design) {
        this.tint = tint;
        this.cornerRadius = cornerRadius;
        this.design = design;
        this.capabilities().all(false);
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        renderer.custom(new GlassPanelCommand(
                0F, 0F, size.width(), size.height(), this.cornerRadius.get(), this.tint.get(), this.design.get()));
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return Size.EMPTY;
    }
}
