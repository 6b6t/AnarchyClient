package net.lenni0451.rivet.input.mouse;

import lombok.With;
import lombok.experimental.WithBy;

@With
@WithBy
public record MouseMoveEvent(float x, float y) {
}
