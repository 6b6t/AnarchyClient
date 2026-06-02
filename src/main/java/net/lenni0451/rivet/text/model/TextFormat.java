package net.lenni0451.rivet.text.model;

import lombok.With;
import lombok.experimental.WithBy;
import net.lenni0451.commons.color.Color;

@With
@WithBy
public record TextFormat(Color color, Color outlineColor, boolean bold, boolean italic, boolean underlined, boolean strikethrough, boolean shadow) {

    public static final TextFormat DEFAULT = new TextFormat(Color.WHITE, Color.TRANSPARENT, false, false, false, false, false);

}
