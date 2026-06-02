package net.lenni0451.rivet.dragdrop;

import lombok.With;
import lombok.experimental.WithBy;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;

@With
@WithBy
public record DragOverEvent(float x, float y, MouseMoveEvent mouseEvent, Object dragData) {
}
