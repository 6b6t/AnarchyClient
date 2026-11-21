package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import net.blockhost.anarchyclient.ui.GlassTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class HudText {

    private HudText() {}

    /** Default HUD text color: the global text token, so every element matches the menu. */
    static int textColor() {
        return GlassTheme.TEXT.toARGB();
    }

    /** Accent HUD text color, for elements that highlight their values. */
    static int accentColor() {
        return GlassTheme.accent().toARGB();
    }

    /** Space between the panel edge and its text. */
    static final int PADDING = 3;

    /**
     * Panel size in pixels, padding included, so the HUD layout clamps and records what is actually drawn:
     * {@code [maxLineWidth + 2 * PADDING, lineCount * 10 + PADDING + 1]}.
     */
    static int[] size(final Minecraft client, final List<String> lines) {
        int width = lines.stream().mapToInt(client.font::width).max().orElse(0);
        return panelSize(width, lines.size());
    }

    static int[] panelSize(final int textWidth, final int lineCount) {
        return new int[] {textWidth + 2 * PADDING, lineCount * 10 + PADDING + 1};
    }

    /** Draw a text panel with its top-left corner at {@code (x, y)}. Positioning is owned by the caller. */
    static void panelAt(
            final Minecraft client,
            final GuiGraphicsExtractor graphics,
            final List<String> lines,
            final int x,
            final int y,
            final int color,
            final boolean background) {
        if (lines.isEmpty()) {
            return;
        }
        int[] size = size(client, lines);
        if (background) {
            graphics.fill(x, y, x + size[0], y + size[1], 0x66000000);
        }
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(client.font, lines.get(index), x + PADDING, y + PADDING + index * 10, color, true);
        }
    }

    /** Legacy corner-anchored panel, still used by non-HUD-editor overlays such as Nametags. */
    static void panel(
            final Minecraft client,
            final GuiGraphicsExtractor graphics,
            final List<String> lines,
            final String corner,
            final int color) {
        if (lines.isEmpty()) {
            return;
        }
        int[] size = size(client, lines);
        int x = corner.endsWith("Right") ? graphics.guiWidth() - size[0] - PADDING : PADDING;
        int y = corner.startsWith("Bottom") ? graphics.guiHeight() - size[1] - PADDING : PADDING;
        panelAt(client, graphics, lines, x, y, color, true);
    }
}
