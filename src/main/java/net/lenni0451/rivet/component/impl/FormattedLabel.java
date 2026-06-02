package net.lenni0451.rivet.component.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.backend.text.ShapedTextBlock;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.TextParser;
import net.lenni0451.rivet.text.TextWrapper;
import net.lenni0451.rivet.text.model.TextFormat;
import net.lenni0451.rivet.text.model.TextLine;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.theme.Theme;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Accessors(fluent = true, chain = true, makeFinal = true)
public class FormattedLabel extends Component {

    @Getter
    @Nullable
    private String text;
    @Getter
    @Nullable
    private TextFormat format;
    @Getter
    @Nullable
    private TextLine line;
    private ShapedText shapedText;
    @Getter
    @Setter
    private TextOrigin.Horizontal horizontalOrigin = TextOrigin.Horizontal.VISUAL_CENTER;
    @Getter
    @Setter
    private TextOrigin.Vertical verticalOrigin = TextOrigin.Vertical.LOGICAL_CENTER;
    @Getter
    private float scale = 1F;

    public FormattedLabel(@Nonnull final String text) {
        this(text, null);
    }

    public FormattedLabel(@Nonnull final String text, @Nullable final TextFormat defaultFormat) {
        this.text = text;
        this.format = defaultFormat;
    }

    public FormattedLabel(@Nonnull final TextLine line) {
        this.line = line;
    }

    public FormattedLabel text(final String text) {
        this.text = text;
        this.line = null;
        if (this.parent() != null) {
            this.parent().requestLayoutRecalculation();
        }
        return this;
    }

    public FormattedLabel text(final String text, final TextFormat defaultFormat) {
        this.text = text;
        this.format = defaultFormat;
        this.line = null;
        if (this.parent() != null) {
            this.parent().requestLayoutRecalculation();
        }
        return this;
    }

    public FormattedLabel text(final TextLine line) {
        this.text = null;
        this.line = line;
        if (this.parent() != null) {
            this.parent().requestLayoutRecalculation();
        }
        return this;
    }

    public FormattedLabel scale(final float scale) {
        if (this.scale != scale) {
            this.scale = scale;
            if (this.parent() != null) {
                this.parent().requestLayoutRecalculation();
            }
        }
        return this;
    }

    private void parseLine() {
        if (this.line == null) {
            TextFormat format = TextFormat.DEFAULT.withColor(this.rivet().theme().get(Theme.TEXT_COLOR));
            this.line = TextParser.parse(this.text, format);
        }
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        float x = this.horizontalOrigin.offset(bounds.width() / this.scale);
        float y = this.verticalOrigin.offset(bounds.height() / this.scale);
        renderer.scale(this.scale, () -> {
            renderer.text(this.shapedText, x, y, this.horizontalOrigin, this.verticalOrigin);
        });
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        this.parseLine();
        ShapedTextBlock shapedTextBlock = TextWrapper.wrapLine(this.rivet().backend(), this.line, constraints.width() / this.scale);
        return new Size(
                shapedTextBlock.visualBounds().width() * this.scale,
                shapedTextBlock.logicalBounds().height() * this.scale
        );
    }

    @Override
    public void computeLayout(final Size size) {
        this.parseLine();
        this.shapedText = TextWrapper.wrapLine(this.rivet().backend(), this.line, size.width() / this.scale);
    }

}
