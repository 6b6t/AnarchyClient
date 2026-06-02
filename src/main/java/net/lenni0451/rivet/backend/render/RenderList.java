package net.lenni0451.rivet.backend.render;

import javax.annotation.Nullable;
import java.util.List;

public record RenderList(@Nullable TransformCommand transform, List<RenderElement> elements) implements RenderElement {

    public RenderList(@Nullable final TransformCommand transform, final List<RenderElement> elements) {
        this.transform = transform;
        this.elements = List.copyOf(elements);
    }

}
