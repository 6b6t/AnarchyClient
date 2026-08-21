package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.ui.GlassTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

final class HudText {

    private HudText() {
    }

    /** Default HUD text color: the global text token, so every element matches the menu. */
    static int textColor() {
        return GlassTheme.TEXT.toARGB();
    }

    /** Accent HUD text color, for elements that highlight their values. */
    static int accentColor() {
        return GlassTheme.accent().toARGB();
    }

    /** Text-block size in pixels: {@code [maxLineWidth, lineCount * 10]}. */
    static int[] size(final Minecraft client, final List<String> lines) {
        int width = lines.stream().mapToInt(client.font::width).max().orElse(0);
        return new int[]{width, lines.size() * 10};
    }

    /** Draw a text panel with its top-left at {@code (x, y)}. Positioning is owned by the caller. */
    static void panelAt(final Minecraft client, final GuiGraphicsExtractor graphics, final List<String> lines,
                        final int x, final int y, final int color, final boolean background) {
        if (lines.isEmpty()) {
            return;
        }
        int width = lines.stream().mapToInt(client.font::width).max().orElse(0);
        if (background) {
            graphics.fill(x - 3, y - 3, x + width + 3, y + lines.size() * 10 + 1, 0x66000000);
        }
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(client.font, lines.get(index), x, y + index * 10, color, true);
        }
    }

    /** Legacy corner-anchored panel, still used by non-HUD-editor overlays such as Nametags. */
    static void panel(final Minecraft client, final GuiGraphicsExtractor graphics, final List<String> lines,
                      final String corner, final int color) {
        if (lines.isEmpty()) {
            return;
        }
        int width = lines.stream().mapToInt(client.font::width).max().orElse(0);
        int x = corner.endsWith("Right") ? graphics.guiWidth() - width - 6 : 6;
        int y = corner.startsWith("Bottom") ? graphics.guiHeight() - lines.size() * 10 - 6 : 6;
        panelAt(client, graphics, lines, x, y, color, true);
    }
}
