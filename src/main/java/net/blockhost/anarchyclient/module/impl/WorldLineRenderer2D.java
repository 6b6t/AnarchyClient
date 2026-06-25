package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

final class WorldLineRenderer2D {

    private WorldLineRenderer2D() {
    }

    static void line(final GuiGraphicsExtractor graphics, final float x0, final float y0, final float x1, final float y1,
                     final int argb) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1F) {
            return;
        }
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate((x0 + x1) * 0.5F, (y0 + y1) * 0.5F).rotate((float) Math.atan2(dy, dx));
        int halfLength = Math.round(length * 0.5F);
        graphics.fill(-halfLength, -1, halfLength, 1, argb);
        pose.popMatrix();
    }
}
