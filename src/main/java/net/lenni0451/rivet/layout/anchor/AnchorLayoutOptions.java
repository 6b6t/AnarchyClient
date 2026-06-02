package net.lenni0451.rivet.layout.anchor;

import lombok.With;
import lombok.experimental.WithBy;
import net.lenni0451.rivet.layout.LayoutOptions;

@With
@WithBy
public record AnchorLayoutOptions(
        float anchorMinX, float anchorMinY, float anchorMaxX, float anchorMaxY,
        float offsetLeft, float offsetTop, float offsetRight, float offsetBottom,
        float pivotX, float pivotY
) implements LayoutOptions {

    public static final AnchorLayoutOptions EMPTY = new AnchorLayoutOptions(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    public AnchorLayoutOptions(final float anchorMinX, final float anchorMinY, final float anchorMaxX, final float anchorMaxY) {
        this(anchorMinX, anchorMinY, anchorMaxX, anchorMaxY, 0, 0, 0, 0, 0, 0);
    }

    public AnchorLayoutOptions point(final float x, final float y) {
        return this.anchor(x, y, x, y);
    }

    public AnchorLayoutOptions from(final float x, final float y) {
        return this.withAnchorMinX(x).withAnchorMinY(y);
    }

    public AnchorLayoutOptions to(final float x, final float y) {
        return this.withAnchorMaxX(x).withAnchorMaxY(y);
    }

    public AnchorLayoutOptions anchor(final float minX, final float minY, final float maxX, final float maxY) {
        return this.from(minX, minY).to(maxX, maxY);
    }

    public AnchorLayoutOptions offset(final float left, final float top, final float right, final float bottom) {
        return this.withOffsetLeft(left).withOffsetTop(top).withOffsetRight(right).withOffsetBottom(bottom);
    }

    public AnchorLayoutOptions pivot(final float x, final float y) {
        return this.withPivotX(x).withPivotY(y);
    }

}
