package net.blockhost.anarchyclient.rivet;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.math.Point;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.text.model.TextFormat;
import net.lenni0451.rivet.text.model.TextLine;
import net.lenni0451.rivet.text.model.TextSection;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

public final class MinecraftShapedText implements ShapedText {

    private final Font font;
    private final int size;
    private final List<Segment> segments;
    private final String text;

    public MinecraftShapedText(final Font font, final int size, final String text, final Color color) {
        this(font, size, List.of(new Segment(text, new TextFormat(color, Color.TRANSPARENT, false, false, false, false, false))));
    }

    public MinecraftShapedText(final Font font, final int size, final TextLine line) {
        this(font, size, fromTextLine(line));
    }

    private MinecraftShapedText(final Font font, final int size, final List<Segment> segments) {
        this.font = font;
        this.size = size;
        this.segments = List.copyOf(segments);
        this.text = flatten(this.segments);
    }

    public Font font() {
        return this.font;
    }

    public int size() {
        return this.size;
    }

    public float scale() {
        return (float) this.size / this.font.lineHeight;
    }

    public List<Segment> segments() {
        return this.segments;
    }

    public String text() {
        return this.text;
    }

    @Override
    public Rectangle visualBounds() {
        return this.logicalBounds();
    }

    @Override
    public Rectangle logicalBounds() {
        return new Rectangle(0, -this.size, this.font.width(this.text) * this.scale(), this.size);
    }

    @Override
    public Point cursorPosition(final int index) {
        int safeIndex = Math.max(0, Math.min(index, this.text.length()));
        return new Point(this.font.width(this.text.substring(0, safeIndex)) * this.scale(), 0);
    }

    @Override
    public int index(final float x, final float y) {
        float unscaledX = x / this.scale();
        float previousWidth = 0;
        for (int index = 0; index < this.text.length(); index++) {
            float nextWidth = this.font.width(this.text.substring(0, index + 1));
            if (unscaledX < previousWidth + (nextWidth - previousWidth) / 2F) {
                return index;
            }
            previousWidth = nextWidth;
        }
        return this.text.length();
    }

    private static List<Segment> fromTextLine(final TextLine line) {
        List<Segment> segments = new ArrayList<>(line.sections().size());
        for (TextSection section : line.sections()) {
            segments.add(new Segment(section.text(), section.format()));
        }
        return segments;
    }

    private static String flatten(final List<Segment> segments) {
        StringBuilder builder = new StringBuilder();
        for (Segment segment : segments) {
            builder.append(segment.text());
        }
        return builder.toString();
    }

    public record Segment(String text, TextFormat format) {
    }
}
