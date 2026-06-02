package net.lenni0451.rivet.component.impl.slider;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layer.Layer;
import net.lenni0451.rivet.layer.LayerBucket;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayout;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayoutOptions;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.theme.Theme;
import net.lenni0451.rivet.theme.ThemeOption;

@Setter
@Accessors(fluent = true, chain = true, makeFinal = true)
public class SliderTooltip extends Component {

    private final ThemeOption<Color> backgroundColor;
    private final ThemeOption<Color> textColor;
    private final ThemeOption<Float> cornerRadius;
    private final ThemeOption<Float> triangleSize;
    private final ThemeOption<Padding> padding;
    private String text;
    private ShapedText shapedText;
    private final Layer layer;
    private float pointerOffset;
    private Position position;

    public SliderTooltip(final String text) {
        this.text = text;

        this.backgroundColor = new ThemeOption<>(this, Theme.SLIDER_TOOLTIP_BACKGROUND_COLOR);
        this.textColor = new ThemeOption<>(this, Theme.SLIDER_TOOLTIP_TEXT_COLOR);
        this.cornerRadius = new ThemeOption<>(this, Theme.SLIDER_TOOLTIP_CORNER_RADIUS);
        this.triangleSize = new ThemeOption<>(this, Theme.SLIDER_TOOLTIP_TRIANGLE_SIZE);
        this.padding = new ThemeOption<>(this, Theme.SLIDER_TOOLTIP_PADDING);

        Container container = new Container(AbsoluteLayout.INSTANCE);
        container.addChild(this);
        this.layer = new Layer(container, LayerBucket.TOOLTIP);
    }

    public void text(final String text) {
        this.text = text;
        this.shapedText = this.rivet().backend().shapeText(text, this.textColor.value());
    }

    public void position(final float x, final float y, final float height) {
        Size idealSize = this.computeIdealSize(null);
        Size screenBounds = this.rivet().scaledSize();

        float posX = x - idealSize.width() / 2F;
        float clampedX = MathUtils.clamp(posX, 0, screenBounds.width() - idealSize.width());
        float pointerOffset = clampedX - posX;
        float posY = y - idealSize.height();
        Position position;
        if (posY < 0) {
            posY = y + height;
            position = Position.BELOW;
        } else {
            position = Position.ABOVE;
        }

        this.update(position, clampedX, posY, pointerOffset);
    }

    private void update(final Position position, final float x, final float y, final float pointerOffset) {
        if (this.layoutOptions() instanceof AbsoluteLayoutOptions options) {
            if (options.x() == x && options.y() == y && this.position.equals(position) && this.pointerOffset == pointerOffset) {
                return;
            }
        }
        this.layoutOptions(new AbsoluteLayoutOptions(x, y));
        this.position = position;
        this.pointerOffset = pointerOffset;
        this.parent().requestLayoutRecalculation();
    }

    public void add(final Rivet rivet) {
        rivet.addLayer(this.layer);
    }

    public void remove() {
        this.rivet().removeLayer(this.layer);
    }

    @Override
    protected void onComponentAdded() {
        this.text(this.text);
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        float triangleSize = this.triangleSize.value();
        Color backgroundColor = this.backgroundColor.value();
        float cornerRadius = this.cornerRadius.value();
        Padding padding = this.padding.value();

        switch (this.position) {
            case ABOVE -> {
                renderer.optimizedFillRoundedRect(0, 0, bounds.width(), bounds.height() - triangleSize, cornerRadius, backgroundColor);
                renderer.fillTriangle(
                        bounds.width() / 2F - triangleSize - this.pointerOffset, bounds.height() - triangleSize,
                        bounds.width() / 2F - this.pointerOffset, bounds.height(),
                        bounds.width() / 2F + triangleSize - this.pointerOffset, bounds.height() - triangleSize,
                        backgroundColor
                );
                renderer.text(this.shapedText, padding.left(), padding.top(), TextOrigin.Horizontal.VISUAL_LEFT, TextOrigin.Vertical.LOGICAL_TOP);
            }
            case BELOW -> {
                renderer.optimizedFillRoundedRect(0, triangleSize, bounds.width(), bounds.height() - triangleSize, cornerRadius, backgroundColor);
                renderer.fillTriangle(
                        bounds.width() / 2F - triangleSize - this.pointerOffset, triangleSize,
                        bounds.width() / 2F + triangleSize - this.pointerOffset, triangleSize,
                        bounds.width() / 2F - this.pointerOffset, 0,
                        backgroundColor
                );
                renderer.text(this.shapedText, padding.left(), triangleSize + padding.top(), TextOrigin.Horizontal.VISUAL_LEFT, TextOrigin.Vertical.LOGICAL_TOP);
            }
        }
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        Padding padding = this.padding.value();
        return new Size(
                this.shapedText.visualBounds().width() + padding.horizontal(),
                this.shapedText.logicalBounds().height() + padding.vertical() + this.triangleSize.value()
        );
    }


    private enum Position {
        ABOVE, BELOW
    }

}
