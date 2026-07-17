package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

import java.util.List;

/**
 * Glass panel mesh with explicit texture coordinates. Unlike {@link Blaze3DGuiShapeRenderState},
 * the UVs are not normalized bounds but a distance field: {@code (u, v)} holds the distance to the
 * nearest vertical/horizontal panel edge, measured in corner-radius units. The glass shader turns
 * that into an exact rounded-rectangle SDF for anti-aliased corners, edge refraction and the rim.
 */
record GlassPanelRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        List<Vertex> vertices,
        int color,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
) implements GuiElementRenderState {

    record Vertex(float x, float y, float u, float v) {
    }

    @Override
    public void buildVertices(final VertexConsumer consumer) {
        for (Vertex vertex : this.vertices) {
            consumer.addVertexWith2DPose(this.pose, vertex.x(), vertex.y())
                    .setUv(vertex.u(), vertex.v())
                    .setColor(this.color);
        }
    }
}
