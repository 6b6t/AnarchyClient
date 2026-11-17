package net.blockhost.anarchyclient.ui;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

import java.util.List;
import java.util.function.Supplier;

import static net.blockhost.anarchyclient.ui.PanelWidgets.drawFittedText;
import static net.blockhost.anarchyclient.ui.PanelWidgets.drawText;
import static net.blockhost.anarchyclient.ui.PanelWidgets.wrapText;

final class TextNode extends Component implements LayoutDebugLabel {

    private static final float LINE_SPACING = 2F;

    private final Supplier<String> text;
    private final Supplier<Color> color;
    private TextOrigin.Horizontal horizontal = TextOrigin.Horizontal.LOGICAL_LEFT;
    private TextOrigin.Vertical vertical = TextOrigin.Vertical.LOGICAL_CENTER;
    int maxLines = 1;

    TextNode(final Supplier<String> text, final Supplier<Color> color) {
        this.text = text;
        this.color = color;
        this.capabilities().all(false);
    }

    TextNode origin(final TextOrigin.Horizontal horizontal, final TextOrigin.Vertical vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
        return this;
    }

    /** Allow the text to fold onto extra lines (word wrap) instead of ellipsizing. */
    TextNode maxLines(final int maxLines) {
        this.maxLines = Math.max(1, maxLines);
        return this;
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        String value = this.text.get();
        if (value.isBlank()) {
            return;
        }
        Color currentColor = this.color.get();
        float x = switch (this.horizontal) {
            case LOGICAL_LEFT, VISUAL_LEFT -> 0F;
            case VISUAL_CENTER -> size.width() / 2F;
            case VISUAL_RIGHT -> size.width();
        };
        if (this.maxLines <= 1) {
            drawFittedText(this, renderer, value, currentColor, x, size.height() / 2F,
                    size.width(), this.horizontal, this.vertical);
            return;
        }
        List<String> lines = wrapText(this, value, currentColor, size.width(), this.maxLines);
        float lineHeight = this.rivet().backend().font().height() + LINE_SPACING;
        float totalHeight = lines.size() * lineHeight - LINE_SPACING;
        float y = (size.height() - totalHeight) / 2F + (lineHeight - LINE_SPACING) / 2F;
        for (String line : lines) {
            drawText(this, renderer, line, currentColor, x, y, this.horizontal, this.vertical);
            y += lineHeight;
        }
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        if (this.rivet() == null) {
            return Size.EMPTY;
        }
        String value = this.text.get();
        if (value.isBlank()) {
            return Size.EMPTY;
        }
        float fontHeight = this.rivet().backend().font().height();
        if (this.maxLines <= 1) {
            // Zero ideal width: text always lives in weighted or fixed-width cells, and a long
            // string's measured width must never squeeze fixed siblings (toggles, icons).
            return new Size(0F, fontHeight);
        }
        int lines = Math.max(1, wrapText(this, value, this.color.get(), constraints.width(), this.maxLines).size());
        return new Size(0F, lines * (fontHeight + LINE_SPACING) - LINE_SPACING);
    }

    /** Number of wrapped lines at the given width; 1 while unattached or for single-line nodes. */
    int lineCount(final float width) {
        if (this.maxLines <= 1 || this.rivet() == null || width <= 0F) {
            return 1;
        }
        String value = this.text.get();
        if (value.isBlank()) {
            return 1;
        }
        return Math.max(1, wrapText(this, value, this.color.get(), width, this.maxLines).size());
    }

    @Override
    public String layoutDebugLabel() {
        return this.text.get();
    }
}