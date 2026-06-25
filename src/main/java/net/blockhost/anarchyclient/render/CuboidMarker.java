package net.blockhost.anarchyclient.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.module.impl.WorldLineRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public record CuboidMarker(String id, AABB box, MarkerStyle style, int lifetimeTicks, int ageTicks) implements Marker {

    public CuboidMarker(final String id, final AABB box, final MarkerStyle style, final int lifetimeTicks) {
        this(id, box, style, lifetimeTicks, 0);
    }

    @Override
    public Marker ticked() {
        return new CuboidMarker(this.id, this.box, this.style, this.lifetimeTicks, this.ageTicks + 1);
    }

    @Override
    public void render(final LevelRenderContext context, final Vec3 camera) {
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (matrices == null || submits == null) {
            return;
        }
        AABB moved = this.box.move(camera.scale(-1));
        if (this.style.hasFill()) {
            WorldLineRenderer.fillNoDepth(matrices, submits, moved, this.style.fill());
        }
        WorldLineRenderer.boxNoDepth(matrices, submits, moved, this.style.outline());
    }
}
