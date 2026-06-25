package net.blockhost.anarchyclient.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.impl.WorldLineRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

public record SphereMarker(String id, Vec3 center, double radius, MarkerStyle style, int lifetimeTicks,
                           int ageTicks) implements Marker {

    private static final int SEGMENTS = 36;

    public SphereMarker(final String id, final Vec3 center, final double radius, final MarkerStyle style,
                        final int lifetimeTicks) {
        this(id, center, radius, style, lifetimeTicks, 0);
    }

    @Override
    public Marker ticked() {
        return new SphereMarker(this.id, this.center, this.radius, this.style, this.lifetimeTicks, this.ageTicks + 1);
    }

    @Override
    public void render(final LevelRenderContext context, final Vec3 camera) {
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (matrices == null || submits == null) {
            return;
        }
        Vec3 moved = this.center.subtract(camera);
        renderCircle(matrices, submits, moved, Axis.Y);
        renderCircle(matrices, submits, moved, Axis.X);
        renderCircle(matrices, submits, moved, Axis.Z);
    }

    private void renderCircle(final PoseStack matrices, final SubmitNodeCollector submits, final Vec3 center,
                              final Axis axis) {
        Vec3 previous = null;
        for (int index = 0; index <= SEGMENTS; index++) {
            double angle = Math.PI * 2.0 * index / SEGMENTS;
            double first = Math.cos(angle) * this.radius;
            double second = Math.sin(angle) * this.radius;
            Vec3 point = switch (axis) {
                case X -> center.add(0.0, first, second);
                case Y -> center.add(first, 0.0, second);
                case Z -> center.add(first, second, 0.0);
            };
            if (previous != null) {
                WorldLineRenderer.lineNoDepth(matrices, submits, previous, point, this.style.outline());
            }
            previous = point;
        }
    }

    private enum Axis {
        X,
        Y,
        Z
    }
}
