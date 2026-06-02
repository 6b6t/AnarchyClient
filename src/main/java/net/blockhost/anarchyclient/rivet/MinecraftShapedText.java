package net.blockhost.anarchyclient.rivet;

import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.text.ShapedTextBlock;
import net.lenni0451.rivet.math.Point;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.text.model.TextBlock;
import net.lenni0451.rivet.text.model.TextFormat;
import net.lenni0451.rivet.text.model.TextLine;
import net.lenni0451.rivet.text.model.TextSection;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.List;

public final class MinecraftShapedText implements ShapedTextBlock {

    private final Font font;
    private final List<Line> lines;
    private final String text;

    public MinecraftShapedText(final Font font, final String text, final Color color) {
        this(font, fromPlainText(text, new TextFormat(color, Color.TRANSPARENT, false, false, false, false, false)));
    }

    public MinecraftShapedText(final Font font, final TextLine line) {
        this(font, List.of(fromTextLine(line)));
    }

    public MinecraftShapedText(final Font font, final TextBlock block) {
        this(font, block.lines().stream().map(MinecraftShapedText::fromTextLine).toList());
    }

    private MinecraftShapedText(final Font font, final List<Line> lines) {
        this.font = font;
        this.lines = lines.isEmpty() ? List.of(new Line(List.of(new Segment("", TextFormat.DEFAULT)))) : List.copyOf(lines);
        this.text = flatten(this.lines);
    }

    public Font font() {
        return this.font;
    }

    public List<Line> lines() {
        return this.lines;
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
        int width = 0;
        for (Line line : this.lines) {
            width = Math.max(width, this.font.width(line.text()));
        }
        return new Rectangle(0, -this.font.lineHeight, width, this.lines.size() * this.font.lineHeight);
    }

    @Override
    public Point cursorPosition(final int index) {
        int safeIndex = Math.max(0, Math.min(index, this.text.length()));
        String beforeCursor = this.text.substring(0, safeIndex);
        int lastBreak = beforeCursor.lastIndexOf('\n');
        int line = (int) beforeCursor.chars().filter(c -> c == '\n').count();
        String currentLine = lastBreak < 0 ? beforeCursor : beforeCursor.substring(lastBreak + 1);
        return new Point(this.font.width(currentLine), line * this.font.lineHeight);
    }

    @Override
    public int index(final float x, final float y) {
        int targetLine = Math.max(0, Math.min(this.lines.size() - 1, (int) (y / this.font.lineHeight)));
        int index = 0;
        for (int line = 0; line < targetLine; line++) {
            index += this.lines.get(line).text().length() + 1;
        }
        String line = this.lines.get(targetLine).text();
        for (int i = 0; i < line.length(); i++) {
            if (this.font.width(line.substring(0, i + 1)) > x) {
                return index + i;
            }
        }
        return index + line.length();
    }

    private static List<Line> fromPlainText(final String text, final TextFormat format) {
        String[] rawLines = text.split("\n", -1);
        List<Line> lines = new ArrayList<>(rawLines.length);
        for (String line : rawLines) {
            lines.add(new Line(List.of(new Segment(line, format))));
        }
        return lines;
    }

    private static Line fromTextLine(final TextLine line) {
        List<Segment> segments = new ArrayList<>(line.sections().size());
        for (TextSection section : line.sections()) {
            segments.add(new Segment(section.text(), section.format()));
        }
        return new Line(segments);
    }

    private static String flatten(final List<Line> lines) {
        StringBuilder builder = new StringBuilder();
        for (Line line : lines) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(line.text());
        }
        return builder.toString();
    }

    public record Line(List<Segment> segments, String text) {

        private Line(final List<Segment> segments) {
            this(List.copyOf(segments), flattenSegments(segments));
        }

        private static String flattenSegments(final List<Segment> segments) {
            StringBuilder builder = new StringBuilder();
            for (Segment segment : segments) {
                builder.append(segment.text());
            }
            return builder.toString();
        }
    }

    public record Segment(String text, TextFormat format) {
    }
}
