package net.lenni0451.rivet.layout.grid;

import lombok.With;
import lombok.experimental.WithBy;
import net.lenni0451.rivet.layout.LayoutOptions;
import net.lenni0451.rivet.math.Padding;

import javax.annotation.Nullable;

@With
@WithBy
public record GridLayoutOptions(
        int column, int row,
        int columnSpan, int rowSpan,
        float weightX, float weightY,
        GridAnchor anchor,
        GridFill fill,
        Padding padding,
        @Nullable Float width, @Nullable Float height
) implements LayoutOptions {

    public static final GridLayoutOptions EMPTY = new GridLayoutOptions(0, 0, 1, 1, 0, 0, GridAnchor.CENTER, GridFill.NONE, Padding.EMPTY, null, null);

    public GridLayoutOptions(final int column, final int row) {
        this(column, row, 1, 1, 0, 0, GridAnchor.CENTER, GridFill.NONE, Padding.EMPTY, null, null);
    }

}
