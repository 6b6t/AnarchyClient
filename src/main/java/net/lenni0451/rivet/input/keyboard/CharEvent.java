package net.lenni0451.rivet.input.keyboard;

import lombok.With;
import lombok.experimental.WithBy;

@With
@WithBy
public record CharEvent(char character) {
}
