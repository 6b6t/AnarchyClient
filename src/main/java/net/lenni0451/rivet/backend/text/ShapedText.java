package net.lenni0451.rivet.backend.text;

import net.lenni0451.rivet.math.Point;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.text.model.TextOrigin;

public interface ShapedText {

    Rectangle visualBounds();

    Rectangle logicalBounds();

    default float offset(final TextOrigin.Horizontal horizontalOrigin) {
        Rectangle visualBounds = this.visualBounds();
        Rectangle logicalBounds = this.logicalBounds();
        return switch (horizontalOrigin) {
            case LOGICAL_LEFT -> -logicalBounds.x();
            case VISUAL_LEFT -> -visualBounds.x();
            case VISUAL_CENTER -> -(visualBounds.x() + visualBounds.maxX()) / 2F;
            case VISUAL_RIGHT -> -visualBounds.maxX();
        };
    }

    default float offset(final TextOrigin.Vertical verticalOrigin) {
        Rectangle visualBounds = this.visualBounds();
        Rectangle logicalBounds = this.logicalBounds();
        return switch (verticalOrigin) {
            case BASELINE -> 0;
            case LOGICAL_TOP -> -logicalBounds.y();
            case LOGICAL_CENTER -> -(logicalBounds.y() + logicalBounds.maxY()) / 2F;
            case LOGICAL_BOTTOM -> -logicalBounds.maxY();
            case VISUAL_TOP -> -visualBounds.y();
            case VISUAL_CENTER -> -(visualBounds.y() + visualBounds.maxY()) / 2F;
            case VISUAL_BOTTOM -> -visualBounds.maxY();
        };
    }

    Point cursorPosition(final int index);

    int index(final float x, final float y);

}
