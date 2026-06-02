package net.lenni0451.rivet.text.model;

import lombok.With;
import lombok.experimental.WithBy;

@With
@WithBy
public record TextSection(String text, TextFormat format) {
}
