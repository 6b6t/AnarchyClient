package net.blockhost.anarchyclient.rivet;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.deferred.RenderCommand;
import net.lenni0451.rivet.math.Rectangle;

/**
 * Draws a liquid glass panel: a rounded rectangle that refracts the blurred game scene behind it.
 *
 * <p>{@code tint} is layered over the refracted background; its alpha controls how strongly the panel
 * is tinted, not the panel's opacity. An optional {@link BackgroundDesign} is rendered on top of the
 * glass as a translucent animated overlay.</p>
 */
public record GlassPanelCommand(
        float x,
        float y,
        float width,
        float height,
        float cornerRadius,
        Color tint,
        BackgroundDesign design
) implements RenderCommand.Custom {

    public GlassPanelCommand(final float x, final float y, final float width, final float height,
                             final float cornerRadius, final Color tint) {
        this(x, y, width, height, cornerRadius, tint, BackgroundDesign.NONE);
    }

    @Override
    public Rectangle bounds() {
        return new Rectangle(this.x, this.y, this.width, this.height);
    }
}
