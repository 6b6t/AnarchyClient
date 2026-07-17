package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.backend.Texture;
import net.lenni0451.rivet.backend.render.CheckedRenderer;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.backend.render.deferred.ModifierCommand;
import net.lenni0451.rivet.backend.render.deferred.RenderCommand;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.math.Point;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class Blaze3DRenderer extends CheckedRenderer {

    private final Minecraft client;
    private final GuiGraphicsExtractor graphics;
    private float xOffset;
    private float yOffset;

    public Blaze3DRenderer(final Minecraft client, final GuiGraphicsExtractor graphics) {
        this.client = client;
        this.graphics = graphics;
    }

    @Override
    public float xOffset() {
        return this.xOffset;
    }

    @Override
    public float yOffset() {
        return this.yOffset;
    }

    @Override
    protected void doTranslate(final float x, final float y, final Runnable renderer) {
        Matrix3x2fStack pose = this.graphics.pose();
        float previousXOffset = this.xOffset;
        float previousYOffset = this.yOffset;
        this.xOffset += x;
        this.yOffset += y;
        pose.pushMatrix();
        pose.translate(x, y);
        try {
            renderer.run();
        } finally {
            pose.popMatrix();
            this.xOffset = previousXOffset;
            this.yOffset = previousYOffset;
        }
    }

    @Override
    protected void doComponentBounds(final float x, final float y, final float width, final float height, final Runnable renderer) {
        this.withScissor(x, y, width, height, renderer);
    }

    @Override
    protected void doScissor(final float x, final float y, final float width, final float height, final Runnable renderer) {
        this.withScissor(x, y, width, height, renderer);
    }

    @Override
    protected void doScale(final float x, final float y, final Runnable renderer) {
        Matrix3x2fStack pose = this.graphics.pose();
        pose.pushMatrix();
        pose.scale(x, y);
        try {
            renderer.run();
        } finally {
            pose.popMatrix();
        }
    }

    @Override
    protected void doStencil(final Consumer<Renderer> maskRenderer, final Runnable renderer) {
        throw new UnsupportedOperationException("Stencil rendering is not supported by the Blaze3D Rivet backend");
    }

    @Override
    protected void doInverseStencil(final Consumer<Renderer> maskRenderer, final Runnable renderer) {
        throw new UnsupportedOperationException("Inverse stencil rendering is not supported by the Blaze3D Rivet backend");
    }

    @Override
    public void custom(final ModifierCommand.Custom command, final Runnable renderer) {
        throw new UnsupportedOperationException("Unsupported Rivet modifier command: " + command.getClass().getName());
    }

    @Override
    protected void doText(final ShapedText shapedText, final float anchorX, final float anchorY,
                          final TextOrigin.Horizontal horizontalOrigin, final TextOrigin.Vertical verticalOrigin) {
        if (!(shapedText instanceof MinecraftShapedText shaped)) {
            throw new UnsupportedOperationException("Unsupported shaped text: " + shapedText.getClass().getName());
        }
        float x = shaped.alignAnchorTo(anchorX, horizontalOrigin, TextOrigin.Horizontal.LOGICAL_LEFT);
        float baselineY = shaped.alignAnchorTo(anchorY, verticalOrigin, TextOrigin.Vertical.BASELINE);
        Matrix3x2fStack pose = this.graphics.pose();
        pose.pushMatrix();
        pose.translate(x, baselineY - shaped.size());
        pose.scale(shaped.scale(), shaped.scale());
        try {
            int cursorX = 0;
            for (MinecraftShapedText.Segment segment : shaped.segments()) {
                if (segment.text().isEmpty()) {
                    continue;
                }
                int color = argb(segment.format().color());
                this.graphics.text(this.client.font, segment.text(), cursorX, 0, color, segment.format().shadow());
                if (segment.format().bold()) {
                    this.graphics.text(this.client.font, segment.text(), cursorX + 1, 0, color, segment.format().shadow());
                }
                int width = this.client.font.width(segment.text());
                if (segment.format().underlined()) {
                    this.fill(cursorX, this.client.font.lineHeight - 1, width, 1, segment.format().color());
                }
                if (segment.format().strikethrough()) {
                    this.fill(cursorX, this.client.font.lineHeight / 2F, width, 1, segment.format().color());
                }
                cursorX += width;
            }
        } finally {
            pose.popMatrix();
        }
    }

    @Override
    protected void doImage(final Texture image, final float x, final float y, final float width, final float height, final Color color) {
        if (!(image instanceof MinecraftTexture texture)) {
            throw new UnsupportedOperationException("Unsupported texture: " + image.getClass().getName());
        }
        this.graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture.identifier(),
                Math.round(x),
                Math.round(y),
                texture.x(),
                texture.y(),
                Math.round(width),
                Math.round(height),
                texture.textureWidth(),
                texture.textureHeight(),
                argb(color)
        );
    }

    @Override
    public void custom(final RenderCommand.Custom command) {
        if (command instanceof Blaze3DRenderCommand custom) {
            custom.action().accept(this.graphics);
            return;
        }
        if (command instanceof GlassPanelCommand glass) {
            this.drawGlassPanel(glass);
            return;
        }
        if (command instanceof SoftShadowCommand shadow) {
            this.drawSoftShadow(shadow);
            return;
        }
        throw new UnsupportedOperationException("Unsupported Rivet render command: " + command.getClass().getName());
    }

    private void drawSoftShadow(final SoftShadowCommand shadow) {
        // The distance-field radius spans the shadow margin plus the panel corner, so the
        // sdf_shadow shader fades continuously from the expanded silhouette inward.
        float margin = shadow.spread();
        float feather = margin + Math.max(1F, shadow.cornerRadius());
        this.submitSdfGrid(
                shadow.x() - margin,
                shadow.y() - margin + shadow.offsetY(),
                shadow.width() + margin * 2F,
                shadow.height() + margin * 2F,
                feather,
                AnarchyClientRenderPipelines.SDF_SHADOW,
                shadow.color()
        );
    }

    private void drawGlassPanel(final GlassPanelCommand glass) {
        float radius = Math.max(1F, Math.min(glass.cornerRadius(), Math.min(glass.width(), glass.height()) / 2F));
        GpuTextureView scene = GlassBackdrop.blurredView();
        if (scene == null) {
            // No captured scene yet (first frame, or blur unavailable): draw a plain translucent panel.
            Color fallback = glass.tint().withAlpha(Math.max(glass.tint().getAlpha(), 224));
            this.submitShape(GuiShapeGeometry.filledRoundedRect(
                    glass.x(), glass.y(), glass.width(), glass.height(), radius, radius, radius, radius, argb(fallback)));
        } else {
            this.submitGlassGrid(glass, radius, scene);
        }
        RenderPipeline design = glass.design() == null ? null : glass.design().pipeline();
        if (design != null) {
            // Animated background design layered over the glass at low opacity.
            Color overlay = Color.fromRGBA(16, 16, 18, 96);
            this.submitShape(GuiShapeGeometry.filledRoundedRect(
                    glass.x(), glass.y(), glass.width(), glass.height(), radius, radius, radius, radius, argb(overlay)),
                    design, true, TextureSetup.noTexture());
        }
    }

    /**
     * Submits the panel as a 3x3-point grid (4 quads) whose UVs carry the distance to the nearest
     * vertical/horizontal edge in corner-radius units. That min-distance field is bilinear within
     * each quadrant, so the shader reconstructs an exact rounded-rect SDF and cuts the corners with
     * anti-aliased alpha instead of tessellated geometry.
     */
    private void submitGlassGrid(final GlassPanelCommand glass, final float radius, final GpuTextureView scene) {
        this.submitDistanceFieldGrid(
                glass.x(), glass.y(), glass.width(), glass.height(), radius,
                AnarchyClientRenderPipelines.GLASS_PANEL,
                TextureSetup.singleTexture(scene, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)),
                argb(glass.tint())
        );
    }

    private void submitSdfGrid(final float x, final float y, final float width, final float height,
                               final float radius, final RenderPipeline pipeline, final Color color) {
        if (width <= 0 || height <= 0 || color.getAlpha() <= 0) {
            return;
        }
        this.submitDistanceFieldGrid(x, y, width, height, radius, pipeline, TextureSetup.noTexture(), argb(color));
    }

    private void submitDistanceFieldGrid(final float x, final float y, final float width, final float height,
                                         final float radius, final RenderPipeline pipeline,
                                         final TextureSetup textureSetup, final int color) {
        float[] xs = {x, x + width / 2F, x + width};
        float[] ys = {y, y + height / 2F, y + height};
        float[] us = {0F, width / 2F / radius, 0F};
        float[] vs = {0F, height / 2F / radius, 0F};

        List<GlassPanelRenderState.Vertex> vertices = new ArrayList<>(16);
        for (int qx = 0; qx < 2; qx++) {
            for (int qy = 0; qy < 2; qy++) {
                // Same winding as GuiShapeGeometry rects: TL, BL, BR, TR.
                vertices.add(new GlassPanelRenderState.Vertex(xs[qx], ys[qy], us[qx], vs[qy]));
                vertices.add(new GlassPanelRenderState.Vertex(xs[qx], ys[qy + 1], us[qx], vs[qy + 1]));
                vertices.add(new GlassPanelRenderState.Vertex(xs[qx + 1], ys[qy + 1], us[qx + 1], vs[qy + 1]));
                vertices.add(new GlassPanelRenderState.Vertex(xs[qx + 1], ys[qy], us[qx + 1], vs[qy]));
            }
        }

        Matrix3x2f pose = new Matrix3x2f(this.graphics.pose());
        ScreenRectangle scissorArea = this.currentScissorArea();
        ScreenRectangle bounds = GuiShapeGeometry.bounds(
                GuiShapeGeometry.solidRect(x, y, width, height, 0), pose, scissorArea);
        if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }
        this.graphics.guiRenderState.addGuiElement(new GlassPanelRenderState(
                pipeline,
                textureSetup,
                pose,
                vertices,
                color,
                scissorArea,
                bounds
        ));
    }

    private void withScissor(final float x, final float y, final float width, final float height, final Runnable renderer) {
        this.enableScissor(x, y, width, height);
        try {
            renderer.run();
        } finally {
            this.graphics.disableScissor();
        }
    }

    private void enableScissor(final float x, final float y, final float width, final float height) {
        this.graphics.enableScissor(MathUtils.floorInt(x), MathUtils.floorInt(y), MathUtils.ceilInt(x + width), MathUtils.ceilInt(y + height));
    }

    @Override
    protected void doFillRoundedRect(final float x, final float y, final float width, final float height,
                                     final float rtl, final float rbl, final float rbr, final float rtr, final Color color) {
        // Uniform radii render through the anti-aliased SDF pipeline; mixed radii (unused by the
        // client UI) keep the tessellated fallback.
        if (rtl == rbl && rbl == rbr && rbr == rtr && rtl >= 1F) {
            float radius = Math.min(rtl, Math.min(width, height) / 2F);
            this.submitSdfGrid(x, y, width, height, radius, AnarchyClientRenderPipelines.SDF_FILL, color);
            return;
        }
        this.submitShape(GuiShapeGeometry.filledRoundedRect(x, y, width, height, rtl, rbl, rbr, rtr, argb(color)));
    }

    @Override
    protected void doOutlineRoundedRect(final float x, final float y, final float width, final float height,
                                        final float rtl, final float rbl, final float rbr, final float rtr,
                                        final float outlineWidth, final Color color) {
        this.submitShape(GuiShapeGeometry.outlinedRoundedRect(x, y, width, height, rtl, rbl, rbr, rtr, outlineWidth, argb(color)));
    }

    @Override
    protected void doOutlineRect(final float x, final float y, final float width, final float height, final float outlineWidth, final Color color) {
        this.fill(x, y, width, outlineWidth, color);
        this.fill(x, y + height - outlineWidth, width, outlineWidth, color);
        this.fill(x, y, outlineWidth, height, color);
        this.fill(x + width - outlineWidth, y, outlineWidth, height, color);
    }

    @Override
    protected void doFillCircle(final float x, final float y, final float radius, final Color color) {
        // A square distance-field grid with radius = half size is an exact anti-aliased circle.
        this.submitSdfGrid(x - radius, y - radius, radius * 2F, radius * 2F, radius, AnarchyClientRenderPipelines.SDF_FILL, color);
    }

    @Override
    protected void doOutlineCircle(final float x, final float y, final float radius, final float outlineWidth, final Color color) {
        this.outlineArc(x, y, radius, outlineWidth, 0, GuiShapeGeometry.FULL_CIRCLE, color);
    }

    @Override
    protected void doFillTriangle(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3, final Color color) {
        this.submitShape(GuiShapeGeometry.filledTriangle(x1, y1, x2, y2, x3, y3, argb(color)));
    }

    @Override
    protected void doFillPolygon(final Point[] points, final Color color) {
        List<GuiShapeGeometry.Vertex> vertices = new ArrayList<>((points.length - 2) * 4);
        Point first = points[0];
        for (int index = 1; index < points.length - 1; index++) {
            Point second = points[index];
            Point third = points[index + 1];
            vertices.addAll(GuiShapeGeometry.filledTriangle(first.x(), first.y(), second.x(), second.y(), third.x(), third.y(), argb(color)));
        }
        this.submitShape(vertices);
    }

    @Override
    protected void doLine(final float x1, final float y1, final float x2, final float y2, final float width, final Color color) {
        this.submitShape(GuiShapeGeometry.line(x1, y1, x2, y2, width, argb(color)));
    }

    @Override
    protected void doPolyLine(final Point[] points, final float width, final Color color) {
        List<GuiShapeGeometry.Vertex> vertices = new ArrayList<>();
        for (int index = 0; index < points.length - 1; index++) {
            Point start = points[index];
            Point end = points[index + 1];
            vertices.addAll(GuiShapeGeometry.line(start.x(), start.y(), end.x(), end.y(), width, argb(color)));
        }
        this.submitShape(vertices);
    }

    @Override
    protected void doFillGradientRect(final float x, final float y, final float width, final float height,
                                      final Color ctl, final Color cbl, final Color cbr, final Color ctr) {
        this.submitShape(GuiShapeGeometry.gradientRect(x, y, width, height, argb(ctl), argb(cbl), argb(cbr), argb(ctr)));
    }

    @Override
    protected void doFillRect(final float x, final float y, final float width, final float height, final Color color) {
        this.fill(x, y, width, height, color);
    }

    private void fill(final float x, final float y, final float width, final float height, final Color color) {
        if (width <= 0 || height <= 0 || color.getAlpha() <= 0) {
            return;
        }
        this.graphics.fill(RenderPipelines.GUI, MathUtils.floorInt(x), MathUtils.floorInt(y), MathUtils.ceilInt(x + width), MathUtils.ceilInt(y + height), argb(color));
    }

    private void outlineArc(final float x, final float y, final float radius, final float outlineWidth,
                            final float startAngle, final float endAngle, final Color color) {
        if (radius <= 0 || outlineWidth <= 0 || color.getAlpha() <= 0) {
            return;
        }
        this.submitShape(GuiShapeGeometry.ringArc(x, y, radius, Math.max(0, radius - outlineWidth), startAngle, endAngle, argb(color)));
    }

    private void submitShape(final List<GuiShapeGeometry.Vertex> vertices) {
        this.submitShape(vertices, RenderPipelines.GUI, false, TextureSetup.noTexture());
    }

    private void submitShape(final List<GuiShapeGeometry.Vertex> vertices, final RenderPipeline pipeline,
                             final boolean useUv, final TextureSetup textureSetup) {
        if (vertices.isEmpty()) {
            return;
        }

        Matrix3x2f pose = new Matrix3x2f(this.graphics.pose());
        ScreenRectangle scissorArea = this.currentScissorArea();
        ScreenRectangle bounds = GuiShapeGeometry.bounds(vertices, pose, scissorArea);
        if (bounds == null || bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }

        this.graphics.guiRenderState.addGuiElement(new Blaze3DGuiShapeRenderState(
                pipeline,
                textureSetup,
                pose,
                vertices,
                useUv,
                scissorArea,
                bounds
        ));
    }

    private ScreenRectangle currentScissorArea() {
        return this.graphics.scissorStack.peek();
    }

    private static int argb(final Color color) {
        return (color.getAlpha() & 0xFF) << 24
                | (color.getRed() & 0xFF) << 16
                | (color.getGreen() & 0xFF) << 8
                | (color.getBlue() & 0xFF);
    }
}
