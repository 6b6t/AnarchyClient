package net.blockhost.anarchyclient.ui;

import java.util.function.Supplier;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Size;

final class Surface extends Component {

    private final Supplier<Color> color;
    private Supplier<Color> outlineColor = () -> Color.TRANSPARENT;
    private float outlineWidth;
    float cornerRadius;

    Surface(final Supplier<Color> color) {
        this.color = color;
        this.capabilities().all(false);
    }

    Surface outline(final Color color, final float width) {
        return this.outline(() -> color, width);
    }

    Surface outline(final Supplier<Color> color, final float width) {
        this.outlineColor = color;
        this.outlineWidth = width;
        return this;
    }

    Surface cornerRadius(final float cornerRadius) {
        this.cornerRadius = cornerRadius;
        return this;
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        Color fill = this.color.get();
        float radius = Math.min(this.cornerRadius, Math.min(size.width(), size.height()) / 2F);
        if (fill.getAlpha() > 0) {
            renderer.optimizedFillRoundedRect(0, 0, size.width(), size.height(), radius, fill);
        }
        Color outline = this.outlineColor.get();
        if (outline.getAlpha() > 0 && this.outlineWidth > 0F) {
            renderer.optimizedOutlineRoundedRect(0, 0, size.width(), size.height(), radius, this.outlineWidth, outline);
        }
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return Size.EMPTY;
    }
}
