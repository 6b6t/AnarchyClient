package net.lenni0451.rivet.input.mouse;

import lombok.With;
import lombok.experimental.WithBy;

@With
@WithBy
public record MouseScrollEvent(float x, float y, float scrollX, float scrollY) {
}
