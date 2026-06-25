package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.blockhost.anarchyclient.rivet.AnarchyClientRenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WorldLineRenderer {

    private WorldLineRenderer() {
    }

    public static void boxNoDepth(final PoseStack matrices, final SubmitNodeCollector submits, final AABB box, final Color color) {
        box(matrices, submits, box, color, AnarchyClientRenderPipelines.LINES_NO_DEPTH);
    }

    private static void box(final PoseStack matrices, final SubmitNodeCollector submits, final AABB box, final Color color,
                            final net.minecraft.client.renderer.rendertype.RenderType renderType) {
        submits.submitCustomGeometry(matrices, renderType, (pose, vertices) -> {
            box(pose, vertices, box, color);
        });
    }

    public static void fillNoDepth(final PoseStack matrices, final SubmitNodeCollector submits, final AABB box, final Color color) {
        submits.submitCustomGeometry(matrices, AnarchyClientRenderPipelines.QUADS_NO_DEPTH, (pose, vertices) -> {
            quad(pose, vertices, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ, color);
            quad(pose, vertices, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
            quad(pose, vertices, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, color);
            quad(pose, vertices, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, box.maxX, box.maxY, box.minZ, color);
            quad(pose, vertices, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, color);
            quad(pose, vertices, box.maxX, box.maxY, box.minZ, box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, color);
        });
    }

    private static void box(final PoseStack.Pose pose, final VertexConsumer vertices, final AABB box, final Color color) {
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
    }

    private static void quad(final PoseStack.Pose pose, final VertexConsumer vertices,
                             final double x1, final double y1, final double z1,
                             final double x2, final double y2, final double z2,
                             final double x3, final double y3, final double z3,
                             final double x4, final double y4, final double z4,
                             final Color color) {
        vertices.addVertex(pose, (float) x1, (float) y1, (float) z1).setColor(color.red(), color.green(), color.blue(), color.alpha());
        vertices.addVertex(pose, (float) x2, (float) y2, (float) z2).setColor(color.red(), color.green(), color.blue(), color.alpha());
        vertices.addVertex(pose, (float) x3, (float) y3, (float) z3).setColor(color.red(), color.green(), color.blue(), color.alpha());
        vertices.addVertex(pose, (float) x4, (float) y4, (float) z4).setColor(color.red(), color.green(), color.blue(), color.alpha());
    }

    public static AABB interpolatedBox(final Entity entity, final float partialTick, final double inflate, final Vec3 camera) {
        double dx = Mth.lerp(partialTick, entity.xo, entity.getX()) - entity.getX();
        double dy = Mth.lerp(partialTick, entity.yo, entity.getY()) - entity.getY();
        double dz = Mth.lerp(partialTick, entity.zo, entity.getZ()) - entity.getZ();
        return entity.getBoundingBox().move(dx, dy, dz).inflate(inflate).move(camera.scale(-1));
    }

    public static void lineNoDepth(final PoseStack matrices, final SubmitNodeCollector submits, final Vec3 start, final Vec3 end,
                                   final Color color) {
        submits.submitCustomGeometry(matrices, AnarchyClientRenderPipelines.LINES_NO_DEPTH,
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

    public record Color(int red, int green, int blue, int alpha) {
    }
}
