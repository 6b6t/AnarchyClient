package net.lenni0451.rivet.layout.absolute;

import lombok.With;
import lombok.experimental.WithBy;
import net.lenni0451.rivet.layout.LayoutOptions;
import net.lenni0451.rivet.math.Rectangle;

import javax.annotation.Nullable;

@With
@WithBy
public record AbsoluteLayoutOptions(float x, float y, @Nullable Float width, @Nullable Float height) implements LayoutOptions {

    public AbsoluteLayoutOptions(final float x, final float y) {
        this(x, y, null, null);
    }

    public AbsoluteLayoutOptions(final Rectangle bounds) {
        this(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

}
