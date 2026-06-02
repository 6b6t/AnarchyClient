package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

import java.util.List;

record Blaze3DGuiShapeRenderState(
        RenderPipeline pipeline,
        TextureSetup textureSetup,
        Matrix3x2fc pose,
        List<GuiShapeGeometry.Vertex> vertices,
        boolean useUv,
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
) implements GuiElementRenderState {

    @Override
    public void buildVertices(final VertexConsumer consumer) {
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        if (this.useUv) {
            for (GuiShapeGeometry.Vertex vertex : this.vertices) {
                minX = Math.min(minX, vertex.x());
                minY = Math.min(minY, vertex.y());
                maxX = Math.max(maxX, vertex.x());
                maxY = Math.max(maxY, vertex.y());
            }
        }

        for (GuiShapeGeometry.Vertex vertex : this.vertices) {
            VertexConsumer next = consumer.addVertexWith2DPose(this.pose, vertex.x(), vertex.y());
            if (this.useUv) {
                float u = maxX > minX ? (vertex.x() - minX) / (maxX - minX) : 0;
                float v = maxY > minY ? (vertex.y() - minY) / (maxY - minY) : 0;
                next.setUv(u, v);
            }
            next.setColor(vertex.color());
        }
    }
}
