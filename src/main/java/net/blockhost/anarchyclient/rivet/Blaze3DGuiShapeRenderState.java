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
        ScreenRectangle scissorArea,
        ScreenRectangle bounds
) implements GuiElementRenderState {

    @Override
    public void buildVertices(final VertexConsumer consumer) {
        for (GuiShapeGeometry.Vertex vertex : this.vertices) {
            consumer.addVertexWith2DPose(this.pose, vertex.x(), vertex.y()).setColor(vertex.color());
        }
    }
}
