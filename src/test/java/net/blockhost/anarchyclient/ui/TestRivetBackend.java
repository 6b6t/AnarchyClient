package net.blockhost.anarchyclient.ui;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.Backend;
import net.lenni0451.rivet.backend.text.Font;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.input.keyboard.Key;
import net.lenni0451.rivet.math.Point;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.text.model.TextLine;

/**
 * Rivet backend for tests. Text has a fixed 9px height and zero width.
 */
final class TestRivetBackend implements Backend {

    private final Font font = new TestFont();

    @Override
    public Font font() {
        return this.font;
    }

    @Override
    public String getClipboard() {
        return null;
    }

    @Override
    public void setClipboard(final String clipboard) {
    }

    @Override
    public boolean isKeyDown(final Key key) {
        return false;
    }

    private static final class TestFont implements Font {

        @Override
        public int size() {
            return 9;
        }

        @Override
        public float height() {
            return 9F;
        }

        @Override
        public Font derive(final int size) {
            return this;
        }

        @Override
        public ShapedText shapeText(final String text, final Color color) {
            return new TestShapedText();
        }

        @Override
        public ShapedText shapeText(final TextLine line) {
            return new TestShapedText();
        }
    }

    private static final class TestShapedText implements ShapedText {

        @Override
        public Rectangle visualBounds() {
            return new Rectangle(0, -9, 0, 9);
        }

        @Override
        public Rectangle logicalBounds() {
            return this.visualBounds();
        }

        @Override
        public Point cursorPosition(final int index) {
            return new Point(0, 0);
        }

        @Override
        public int index(final float x, final float y) {
            return 0;
        }
    }
}
