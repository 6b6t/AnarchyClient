package net.blockhost.anarchyclient.rivet;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.deferred.RenderCommand;
import net.lenni0451.rivet.math.Rectangle;

/**
 * Soft drop shadow under a rounded panel: a single distance-field draw whose alpha fades
 * continuously from the panel silhouette outward over {@code spread} pixels.
 */
public record SoftShadowCommand(
        float x,
        float y,
        float width,
        float height,
        float cornerRadius,
        float spread,
        float offsetY,
        Color color
) implements RenderCommand.Custom {

    @Override
    public Rectangle bounds() {
        return new Rectangle(this.x - this.spread, this.y - this.spread + this.offsetY,
                this.width + this.spread * 2F, this.height + this.spread * 2F);
    }
}
