package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class WorldLineRenderer {

    private WorldLineRenderer() {
    }

    static void box(final PoseStack matrices, final SubmitNodeCollector submits, final AABB box, final Color color) {
        submits.submitCustomGeometry(matrices, RenderTypes.lines(), (pose, vertices) -> {
            line(pose, vertices, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, color);
            line(pose, vertices, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, color);
            line(pose, vertices, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
            line(pose, vertices, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, color);

            line(pose, vertices, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, color);
            line(pose, vertices, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, color);
            line(pose, vertices, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
            line(pose, vertices, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, color);

            line(pose, vertices, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, color);
            line(pose, vertices, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, color);
            line(pose, vertices, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
            line(pose, vertices, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, color);
        });
    }

    static void line(final PoseStack matrices, final SubmitNodeCollector submits, final Vec3 start, final Vec3 end, final Color color) {
        submits.submitCustomGeometry(matrices, RenderTypes.lines(),
                (pose, vertices) -> line(pose, vertices, start.x, start.y, start.z, end.x, end.y, end.z, color));
    }

    private static void line(final PoseStack.Pose pose, final VertexConsumer vertices, final double x1, final double y1, final double z1,
                             final double x2, final double y2, final double z2, final Color color) {
        float normalX = (float) (x2 - x1);
        float normalY = (float) (y2 - y1);
        float normalZ = (float) (z2 - z1);
        float length = (float) Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (length > 0) {
            normalX /= length;
            normalY /= length;
            normalZ /= length;
        }

        vertices.addVertex(pose, (float) x1, (float) y1, (float) z1)
                .setColor(color.red(), color.green(), color.blue(), color.alpha())
                .setLineWidth(1.0F)
                .setNormal(pose, normalX, normalY, normalZ);
        vertices.addVertex(pose, (float) x2, (float) y2, (float) z2)
                .setColor(color.red(), color.green(), color.blue(), color.alpha())
                .setLineWidth(1.0F)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    record Color(int red, int green, int blue, int alpha) {
    }
}
