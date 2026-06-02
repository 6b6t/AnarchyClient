package net.lenni0451.rivet.math;

import lombok.experimental.UtilityClass;
import net.lenni0451.rivet.Rivet;

@UtilityClass
public final class Snapping {

    public static float snap(final Rivet rivet, final float value) {
        if (!rivet.snapToInteger()) return value;
        return Math.round(value);
    }

    public static Rectangle snap(final Rivet rivet, final Rectangle rect) {
        if (!rivet.snapToInteger()) return rect;
        float x = (float) Math.floor(rect.x());
        float y = (float) Math.floor(rect.y());
        return new Rectangle(
                x,
                y,
                (float) Math.ceil(rect.maxX()) - x,
                (float) Math.ceil(rect.maxY()) - y
        );
    }

}
