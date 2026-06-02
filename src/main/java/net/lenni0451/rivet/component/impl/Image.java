package net.lenni0451.rivet.component.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.Texture;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

@RequiredArgsConstructor
@Accessors(fluent = true, chain = true, makeFinal = true)
public class Image extends Component {

    @Getter
    private final Texture texture;
    @Getter
    @Setter
    private Color color = Color.WHITE;

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        renderer.image(this.texture, 0, 0, bounds.width(), bounds.height(), this.color);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(this.texture.width(), this.texture.height());
    }

}
