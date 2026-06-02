package net.lenni0451.rivet.component.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import java.util.function.Consumer;

@Getter
@Setter
@Accessors(fluent = true, chain = true, makeFinal = true)
public class SolidColor extends Component {

    private Color color = Color.TRANSPARENT;
    private Color outlineColor = Color.TRANSPARENT;
    private float outlineWidth = -1;
    private float cornerRadius = 0;

    public SolidColor() {
        this(s -> {});
    }

    public SolidColor(final Consumer<SolidColor> initializer) {
        initializer.accept(this);
    }

    @Override
    protected void onComponentAdded() {
        if (this.outlineWidth == -1) {
            this.outlineWidth = this.rivet().backend().getTextHeight() / 8F;
        }
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        if (this.color.getAlpha() > 0) {
            renderer.optimizedFillRoundedRect(0, 0, bounds.width(), bounds.height(), this.cornerRadius, this.color);
        }
        if (this.outlineColor.getAlpha() > 0 && this.outlineWidth > 0) {
            renderer.optimizedOutlineRoundedRect(0, 0, bounds.width(), bounds.height(), this.cornerRadius, this.outlineWidth, this.outlineColor);
        }
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return Size.EMPTY;
    }

}
