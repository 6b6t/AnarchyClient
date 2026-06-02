package net.lenni0451.rivet.component.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.theme.Theme;

@Accessors(fluent = true, chain = true, makeFinal = true)
public class Label extends Component {

    @Getter
    private String text;
    private ShapedText shapedText;
    private boolean reshape;
    @Getter
    @Setter
    private TextOrigin.Horizontal horizontalOrigin = TextOrigin.Horizontal.VISUAL_CENTER;
    @Getter
    @Setter
    private TextOrigin.Vertical verticalOrigin = TextOrigin.Vertical.LOGICAL_CENTER;
    @Getter
    private float scale = 1F;

    public Label(final String text) {
        this.text = text;
    }

    public Label text(final String text) {
        if (!this.text.equals(text)) {
            this.text = text;
            this.reshape = true;
            if (this.parent() != null) {
                this.parent().requestLayoutRecalculation();
            }
        }
        return this;
    }

    public Label scale(final float scale) {
        if (this.scale != scale) {
            this.scale = scale;
            if (this.parent() != null) {
                this.parent().requestLayoutRecalculation();
            }
        }
        return this;
    }

    private void shapeText() {
        if (this.reshape) {
            this.shapedText = this.rivet().backend().shapeText(this.text, this.rivet().theme().get(Theme.TEXT_COLOR));
            this.reshape = false;
        }
    }

    @Override
    protected void onComponentAdded() {
        this.reshape = true;
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        this.shapeText();
        float x = this.horizontalOrigin.offset(bounds.width() / this.scale);
        float y = this.verticalOrigin.offset(bounds.height() / this.scale);
        renderer.scale(this.scale, () -> renderer.text(this.shapedText, x, y, this.horizontalOrigin, this.verticalOrigin));
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        this.shapeText();
        return new Size(
                this.shapedText.visualBounds().width() * this.scale,
                this.shapedText.logicalBounds().height() * this.scale
        );
    }

}
