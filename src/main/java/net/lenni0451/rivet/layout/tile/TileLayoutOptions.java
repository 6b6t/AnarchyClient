package net.lenni0451.rivet.layout.tile;

import lombok.With;
import lombok.experimental.WithBy;
import net.lenni0451.rivet.layout.LayoutOptions;

@With
@WithBy
public record TileLayoutOptions(int column, int row) implements LayoutOptions {
}
