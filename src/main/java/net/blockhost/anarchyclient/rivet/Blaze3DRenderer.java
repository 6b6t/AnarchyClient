package net.blockhost.anarchyclient.rivet;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.RenderCommand;
import net.lenni0451.rivet.backend.render.RenderElement;
import net.lenni0451.rivet.backend.render.RenderList;
import net.lenni0451.rivet.backend.render.TransformCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2fStack;

public final class Blaze3DRenderer {

    private final Minecraft client;
    private final GuiGraphicsExtractor graphics;

    public Blaze3DRenderer(final Minecraft client, final GuiGraphicsExtractor graphics) {
        this.client = client;
        this.graphics = graphics;
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
            case TransformCommand.ComponentBounds bounds -> this.graphics.enableScissor(
                    Math.round(bounds.x()),
                    Math.round(bounds.y()),
                    Math.round(bounds.x() + bounds.width()),
                    Math.round(bounds.y() + bounds.height())
            );
            case TransformCommand.Scissor scissor -> this.graphics.enableScissor(
                    Math.round(scissor.x()),
                    Math.round(scissor.y()),
                    Math.round(scissor.x() + scissor.width()),
                    Math.round(scissor.y() + scissor.height())
            );
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
            case RenderCommand.OutlineRect rect -> this.graphics.outline(Math.round(rect.x()), Math.round(rect.y()), Math.round(rect.width()), Math.round(rect.height()), argb(rect.color()));
            case RenderCommand.FillRoundedRect rect -> this.fill(rect.x(), rect.y(), rect.width(), rect.height(), rect.color());
            case RenderCommand.OutlineRoundedRect rect -> this.graphics.outline(Math.round(rect.x()), Math.round(rect.y()), Math.round(rect.width()), Math.round(rect.height()), argb(rect.color()));
            case RenderCommand.FillCircle circle -> this.fill(circle.x() - circle.radius(), circle.y() - circle.radius(), circle.radius() * 2, circle.radius() * 2, circle.color());
            case RenderCommand.OutlineCircle circle -> this.graphics.outline(Math.round(circle.x() - circle.radius()), Math.round(circle.y() - circle.radius()), Math.round(circle.radius() * 2), Math.round(circle.radius() * 2), argb(circle.color()));
            case RenderCommand.FillTriangle triangle -> this.fill(Math.min(triangle.x1(), Math.min(triangle.x2(), triangle.x3())), Math.min(triangle.y1(), Math.min(triangle.y2(), triangle.y3())), Math.max(1, Math.abs(triangle.x2() - triangle.x1())), Math.max(1, Math.abs(triangle.y3() - triangle.y1())), triangle.color());
            case RenderCommand.Line line -> this.fill(Math.min(line.x1(), line.x2()), Math.min(line.y1(), line.y2()), Math.max(line.width(), Math.abs(line.x2() - line.x1())), Math.max(line.width(), Math.abs(line.y2() - line.y1())), line.color());
            case RenderCommand.FillGradientRect rect -> this.graphics.fillGradient(Math.round(rect.x()), Math.round(rect.y()), Math.round(rect.x() + rect.width()), Math.round(rect.y() + rect.height()), argb(rect.ctl()), argb(rect.cbr()));
            case RenderCommand.Text text -> {
                if (text.shapedText() instanceof MinecraftShapedText shaped) {
                    String[] lines = shaped.text().split("\n", -1);
                    for (int i = 0; i < lines.length; i++) {
                        this.graphics.text(this.client.font, lines[i], Math.round(text.x()), Math.round(text.y() + i * this.client.font.lineHeight), argb(shaped.color()), false);
                    }
                }
            }
            case RenderCommand.Image ignored -> {
            }
            case RenderCommand.CustomRenderCommand<?> custom -> this.renderCustom(custom);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void renderCustom(final RenderCommand.CustomRenderCommand<?> custom) {
        ((RenderCommand.CustomRenderCommand) custom).action().accept(this.graphics);
    }

    private void fill(final float x, final float y, final float width, final float height, final Color color) {
        this.graphics.fill((RenderPipeline) RenderPipelines.GUI, Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + height), argb(color));
    }

    private static int argb(final Color color) {
        return (color.getAlpha() & 0xFF) << 24
                | (color.getRed() & 0xFF) << 16
                | (color.getGreen() & 0xFF) << 8
                | (color.getBlue() & 0xFF);
    }
}
