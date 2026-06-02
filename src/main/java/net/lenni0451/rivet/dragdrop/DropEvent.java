package net.lenni0451.rivet.dragdrop;

import lombok.With;
import lombok.experimental.WithBy;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;

@With
@WithBy
public record DropEvent(float x, float y, MouseButtonEvent mouseEvent, Object dragData) {
}
