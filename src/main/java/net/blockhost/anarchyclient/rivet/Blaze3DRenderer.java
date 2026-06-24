package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.backend.render.RenderCommand;
import net.lenni0451.rivet.backend.render.RenderElement;
import net.lenni0451.rivet.backend.render.RenderList;
import net.lenni0451.rivet.backend.render.TransformCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.List;

public final class Blaze3DRenderer {

    private final Minecraft client;
    private final GuiGraphicsExtractor graphics;
    private final RenderPipeline backgroundPipeline;

    public Blaze3DRenderer(final Minecraft client, final GuiGraphicsExtractor graphics, final BackgroundDesign background) {
        this.client = client;
        this.graphics = graphics;
        this.backgroundPipeline = background.pipeline();
    }

    public void render(final RenderList renderList) {
        this.renderList(renderList);
    }

    private void renderList(final RenderList renderList) {
        Matrix3x2fStack pose = this.graphics.pose();
        switch (renderList.transform()) {
            case TransformCommand.Translate translate -> {
                pose.pushMatrix();
                pose.translate(translate.x(), translate.y());
            }
            case TransformCommand.Scale scale -> {
                pose.pushMatrix();
                pose.scale(scale.x(), scale.y());
            }
            case TransformCommand.ComponentBounds bounds -> this.enableScissor(bounds.x(), bounds.y(), bounds.width(), bounds.height());
            case TransformCommand.Scissor scissor -> this.enableScissor(scissor.x(), scissor.y(), scissor.width(), scissor.height());
            case null -> {
            }
        }

        for (RenderElement element : renderList.elements()) {
            switch (element) {
                case RenderCommand command -> this.renderCommand(command);
                case RenderList child -> this.renderList(child);
            }
        }

        switch (renderList.transform()) {
            case TransformCommand.Translate _, TransformCommand.Scale _ -> pose.popMatrix();
            case TransformCommand.ComponentBounds _, TransformCommand.Scissor _ -> this.graphics.disableScissor();
            case null -> {
            }
        }
    }

    private void renderCommand(final RenderCommand command) {
        switch (command) {
            case RenderCommand.FillRect rect -> this.fill(rect.x(), rect.y(), rect.width(), rect.height(), rect.color());
            case RenderCommand.OutlineRect rect -> this.outlineRect(rect.x(), rect.y(), rect.width(), rect.height(), rect.outlineWidth(), rect.color());
            case RenderCommand.FillRoundedRect rect -> this.fillRoundedRect(rect.x(), rect.y(), rect.width(), rect.height(), rect.cornerRadius(), rect.color());
            case RenderCommand.OutlineRoundedRect rect -> this.outlineRoundedRect(rect.x(), rect.y(), rect.width(), rect.height(), rect.cornerRadius(), rect.outlineWidth(), rect.color());
            case RenderCommand.FillCircle circle -> this.fillCircle(circle.x(), circle.y(), circle.radius(), circle.color());
            case RenderCommand.OutlineCircle circle -> this.outlineCircle(circle.x(), circle.y(), circle.radius(), circle.outlineWidth(), circle.color());
            case RenderCommand.FillTriangle triangle -> this.fillTriangle(triangle.x1(), triangle.y1(), triangle.x2(), triangle.y2(), triangle.x3(), triangle.y3(), triangle.color());
            case RenderCommand.Line line -> this.line(line.x1(), line.y1(), line.x2(), line.y2(), line.width(), line.color());
            case RenderCommand.FillGradientRect rect -> this.fillGradientRect(rect.x(), rect.y(), rect.width(), rect.height(), rect.ctl(), rect.cbl(), rect.cbr(), rect.ctr());
            case RenderCommand.Text text -> this.text(text);
            case RenderCommand.Image image -> this.image(image);
            case RenderCommand.CustomRenderCommand<?> custom -> this.renderCustom(custom);
        }
    }

    private void text(final RenderCommand.Text text) {
        if (!(text.shapedText() instanceof MinecraftShapedText shaped)) {
            throw new UnsupportedOperationException("Unsupported shaped text: " + text.shapedText().getClass().getName());
        }
        int x = Math.round(text.x() + shaped.visualBounds().x());
        int y = Math.round(text.y() + shaped.visualBounds().y());
        for (int lineIndex = 0; lineIndex < shaped.lines().size(); lineIndex++) {
            int cursorX = x;
            int baselineY = y + lineIndex * this.client.font.lineHeight;
            for (MinecraftShapedText.Segment segment : shaped.lines().get(lineIndex).segments()) {
                if (segment.text().isEmpty()) {
                    continue;
                }
                int color = argb(segment.format().color());
                this.graphics.text(this.client.font, segment.text(), cursorX, baselineY, color, segment.format().shadow());
                if (segment.format().bold()) {
                    this.graphics.text(this.client.font, segment.text(), cursorX + 1, baselineY, color, segment.format().shadow());
                }
                int width = this.client.font.width(segment.text());
                if (segment.format().underlined()) {
                    this.fill(cursorX, baselineY + this.client.font.lineHeight - 1, width, 1, segment.format().color());
                }
                if (segment.format().strikethrough()) {
                    this.fill(cursorX, baselineY + this.client.font.lineHeight / 2F, width, 1, segment.format().color());
                }
                cursorX += width;
            }
        }
    }

    private void image(final RenderCommand.Image image) {
        if (!(image.texture() instanceof MinecraftTexture texture)) {
            throw new UnsupportedOperationException("Unsupported texture: " + image.texture().getClass().getName());
        }
        this.graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                texture.identifier(),
                Math.round(image.x()),
                Math.round(image.y()),
                texture.x(),
                texture.y(),
                Math.round(image.width()),
                Math.round(image.height()),
                texture.textureWidth(),
                texture.textureHeight()
        );
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void renderCustom(final RenderCommand.CustomRenderCommand<?> custom) {
        ((RenderCommand.CustomRenderCommand) custom).action().accept(this.graphics);
    }

    private void enableScissor(final float x, final float y, final float width, final float height) {
        this.graphics.enableScissor(MathUtils.floorInt(x), MathUtils.floorInt(y), MathUtils.ceilInt(x + width), MathUtils.ceilInt(y + height));
    }

    private void fillRoundedRect(final float x, final float y, final float width, final float height, final float cornerRadius, final Color color) {
        if (width <= 0 || height <= 0 || color.getAlpha() <= 0) {
            return;
        }

        this.submitSurfaceShape(GuiShapeGeometry.filledRoundedRect(x, y, width, height, cornerRadius, argb(color)), width, height, color);
    }

    private void outlineRoundedRect(final float x, final float y, final float width, final float height, final float cornerRadius, final float outlineWidth, final Color color) {
        if (width <= 0 || height <= 0 || outlineWidth <= 0 || color.getAlpha() <= 0) {
            return;
        }

        this.submitShape(GuiShapeGeometry.outlinedRoundedRect(x, y, width, height, cornerRadius, outlineWidth, argb(color)));
    }

    private void outlineRect(final float x, final float y, final float width, final float height, final float outlineWidth, final Color color) {
        if (width <= 0 || height <= 0 || outlineWidth <= 0 || color.getAlpha() <= 0) {
            return;
        }

        this.fill(x, y, width, outlineWidth, color);
        this.fill(x, y + height - outlineWidth, width, outlineWidth, color);
        this.fill(x, y, outlineWidth, height, color);
        this.fill(x + width - outlineWidth, y, outlineWidth, height, color);
    }

    private void fillCircle(final float x, final float y, final float radius, final Color color) {
        if (radius <= 0 || color.getAlpha() <= 0) {
            return;
        }
        this.submitShape(GuiShapeGeometry.filledCircle(x, y, radius, argb(color)));
    }

    private void outlineCircle(final float x, final float y, final float radius, final float outlineWidth, final Color color) {
        if (radius <= 0 || outlineWidth <= 0 || color.getAlpha() <= 0) {
            return;
        }
        this.outlineArc(x, y, radius, outlineWidth, 0, GuiShapeGeometry.FULL_CIRCLE, color);
    }

    private void fillTriangle(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3, final Color color) {
        if (color.getAlpha() <= 0) {
            return;
        }
        this.submitShape(GuiShapeGeometry.filledTriangle(x1, y1, x2, y2, x3, y3, argb(color)));
    }

    private void line(final float x1, final float y1, final float x2, final float y2, final float width, final Color color) {
        if (width <= 0 || color.getAlpha() <= 0) {
            return;
        }
        this.submitShape(GuiShapeGeometry.line(x1, y1, x2, y2, width, argb(color)));
    }

    private void fillGradientRect(final float x, final float y, final float width, final float height, final Color ctl, final Color cbl, final Color cbr, final Color ctr) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (ctl.getAlpha() <= 0 && cbl.getAlpha() <= 0 && cbr.getAlpha() <= 0 && ctr.getAlpha() <= 0) {
            return;
        }
        this.submitShape(GuiShapeGeometry.gradientRect(x, y, width, height, argb(ctl), argb(cbl), argb(cbr), argb(ctr)));
    }

    private void fill(final float x, final float y, final float width, final float height, final Color color) {
        if (width <= 0 || height <= 0 || color.getAlpha() <= 0) {
            return;
        }
        if (this.backgroundPipeline != null && this.isPanelSurface(width, height, color)) {
            this.submitShape(GuiShapeGeometry.solidRect(x, y, width, height, argb(color)), this.backgroundPipeline, true);
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
        this.submitShape(vertices, RenderPipelines.GUI, false);
    }

    private void submitSurfaceShape(final List<GuiShapeGeometry.Vertex> vertices, final float width, final float height, final Color color) {
        if (this.backgroundPipeline != null && this.isPanelSurface(width, height, color)) {
            this.submitShape(vertices, this.backgroundPipeline, true);
            return;
        }
        this.submitShape(vertices);
    }

    private void submitShape(final List<GuiShapeGeometry.Vertex> vertices, final RenderPipeline pipeline, final boolean useUv) {
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
                TextureSetup.noTexture(),
                pose,
                vertices,
                useUv,
                scissorArea,
                bounds
        ));
    }

    private boolean isPanelSurface(final float width, final float height, final Color color) {
        if (width < 8 || height < 8) {
            return false;
        }
        int alpha = color.getAlpha();
        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();
        int max = Math.max(red, Math.max(green, blue));
        int min = Math.min(red, Math.min(green, blue));
        return alpha >= 90 && max >= 10 && max <= 64 && max - min <= 12;
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
