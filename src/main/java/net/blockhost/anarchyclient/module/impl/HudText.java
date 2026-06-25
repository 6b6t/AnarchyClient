package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;

final class HudText {

    private HudText() {
    }

    static void panel(final Minecraft client, final GuiGraphicsExtractor graphics, final List<String> lines,
                      final String corner, final int color) {
        panel(client, graphics, lines, corner, 0, 0, color, true);
    }

    static void panel(final Minecraft client, final GuiGraphicsExtractor graphics, final List<String> lines,
                      final String corner, final int xOffset, final int yOffset, final int color,
                      final boolean background) {
        if (lines.isEmpty()) {
            return;
        }
        int width = lines.stream().mapToInt(client.font::width).max().orElse(0);
        int x = corner.endsWith("Right") ? graphics.guiWidth() - width - 6 - xOffset : 6 + xOffset;
        int y = corner.startsWith("Bottom") ? graphics.guiHeight() - lines.size() * 10 - 6 - yOffset : 6 + yOffset;
        if (background) {
            graphics.fill(x - 3, y - 3, x + width + 3, y + lines.size() * 10 + 1, 0x66000000);
        }
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(client.font, lines.get(index), x, y + index * 10, color, true);
        }
    }
}
