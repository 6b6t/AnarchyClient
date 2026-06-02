package net.blockhost.anarchyclient.rivet;

import net.lenni0451.rivet.backend.text.ShapedTextBlock;
import net.lenni0451.rivet.math.Point;
import net.lenni0451.rivet.math.Rectangle;
import net.minecraft.client.gui.Font;

public record MinecraftShapedText(Font font, String text) implements ShapedTextBlock {

    @Override
    public Rectangle visualBounds() {
        return this.logicalBounds();
    }

    @Override
    public Rectangle logicalBounds() {
        String[] lines = this.text.split("\n", -1);
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, this.font.width(line));
        }
        return new Rectangle(0, -this.font.lineHeight, width, lines.length * this.font.lineHeight);
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
        String[] lines = this.text.split("\n", -1);
        int targetLine = Math.max(0, Math.min(lines.length - 1, (int) (y / this.font.lineHeight)));
        int index = 0;
        for (int line = 0; line < targetLine; line++) {
            index += lines[line].length() + 1;
        }
        String line = lines[targetLine];
        for (int i = 0; i < line.length(); i++) {
            if (this.font.width(line.substring(0, i + 1)) > x) {
                return index + i;
            }
        }
        return index + line.length();
    }
}
