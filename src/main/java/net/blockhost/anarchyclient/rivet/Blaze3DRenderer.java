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
        this.graphics.enableScissor(floor(x), floor(y), ceil(x + width), ceil(y + height));
    }

    private void fillRoundedRect(final float x, final float y, final float width, final float height, final float cornerRadius, final Color color) {
        float radius = Math.max(0, Math.min(cornerRadius, Math.min(width, height) / 2F));
        if (radius <= 0) {
            this.fill(x, y, width, height, color);
            return;
        }

        this.fill(x + radius, y, width - radius * 2, height, color);
        this.fill(x, y + radius, width, height - radius * 2, color);
        this.fillCircle(x + radius, y + radius, radius, color);
        this.fillCircle(x + width - radius, y + radius, radius, color);
        this.fillCircle(x + radius, y + height - radius, radius, color);
        this.fillCircle(x + width - radius, y + height - radius, radius, color);
    }

    private void outlineRoundedRect(final float x, final float y, final float width, final float height, final float cornerRadius, final float outlineWidth, final Color color) {
        float radius = Math.max(0, Math.min(cornerRadius, Math.min(width, height) / 2F));
        if (radius <= 0) {
            this.outlineRect(x, y, width, height, outlineWidth, color);
            return;
        }

        this.fill(x + radius, y, width - radius * 2, outlineWidth, color);
        this.fill(x + radius, y + height - outlineWidth, width - radius * 2, outlineWidth, color);
        this.fill(x, y + radius, outlineWidth, height - radius * 2, color);
        this.fill(x + width - outlineWidth, y + radius, outlineWidth, height - radius * 2, color);
        this.outlineCircle(x + radius, y + radius, radius, outlineWidth, color);
        this.outlineCircle(x + width - radius, y + radius, radius, outlineWidth, color);
        this.outlineCircle(x + radius, y + height - radius, radius, outlineWidth, color);
        this.outlineCircle(x + width - radius, y + height - radius, radius, outlineWidth, color);
    }

    private void outlineRect(final float x, final float y, final float width, final float height, final float outlineWidth, final Color color) {
        this.fill(x, y, width, outlineWidth, color);
        this.fill(x, y + height - outlineWidth, width, outlineWidth, color);
        this.fill(x, y, outlineWidth, height, color);
        this.fill(x + width - outlineWidth, y, outlineWidth, height, color);
    }

    private void fillCircle(final float x, final float y, final float radius, final Color color) {
        int minY = floor(y - radius);
        int maxY = ceil(y + radius);
        for (int py = minY; py < maxY; py++) {
            double dy = py + 0.5D - y;
            double halfWidth = Math.sqrt(Math.max(0, radius * radius - dy * dy));
            this.fill((float) (x - halfWidth), py, (float) (halfWidth * 2), 1, color);
        }
    }

    private void outlineCircle(final float x, final float y, final float radius, final float outlineWidth, final Color color) {
        if (outlineWidth <= 0) {
            return;
        }
        int samples = Math.max(24, (int) Math.ceil(radius * 8));
        for (int i = 0; i < samples; i++) {
            double angle = Math.PI * 2D * i / samples;
            float px = (float) (x + Math.cos(angle) * radius);
            float py = (float) (y + Math.sin(angle) * radius);
            this.fill(px - outlineWidth / 2F, py - outlineWidth / 2F, outlineWidth, outlineWidth, color);
        }
    }

    private void fillTriangle(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3, final Color color) {
        int minY = floor(Math.min(y1, Math.min(y2, y3)));
        int maxY = ceil(Math.max(y1, Math.max(y2, y3)));
        for (int py = minY; py < maxY; py++) {
            float y = py + 0.5F;
            float[] intersections = new float[3];
            int count = 0;
            count = addIntersection(intersections, count, x1, y1, x2, y2, y);
            count = addIntersection(intersections, count, x2, y2, x3, y3, y);
            count = addIntersection(intersections, count, x3, y3, x1, y1, y);
            if (count >= 2) {
                float minX = Math.min(intersections[0], intersections[1]);
                float maxX = Math.max(intersections[0], intersections[1]);
                this.fill(minX, py, Math.max(1, maxX - minX), 1, color);
            }
        }
    }

    private void line(final float x1, final float y1, final float x2, final float y2, final float width, final Color color) {
        if (Math.abs(y1 - y2) < 0.001F) {
            this.fill(Math.min(x1, x2), y1 - width / 2F, Math.abs(x2 - x1), width, color);
            return;
        }
        if (Math.abs(x1 - x2) < 0.001F) {
            this.fill(x1 - width / 2F, Math.min(y1, y2), width, Math.abs(y2 - y1), color);
            return;
        }

        float dx = x2 - x1;
        float dy = y2 - y1;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(dx), Math.abs(dy))));
        for (int i = 0; i <= steps; i++) {
            float progress = (float) i / steps;
            this.fill(x1 + dx * progress - width / 2F, y1 + dy * progress - width / 2F, width, width, color);
        }
    }

    private void fillGradientRect(final float x, final float y, final float width, final float height, final Color ctl, final Color cbl, final Color cbr, final Color ctr) {
        if (ctl.equals(ctr) && cbl.equals(cbr)) {
            this.graphics.fillGradient(floor(x), floor(y), ceil(x + width), ceil(y + height), argb(ctl), argb(cbl));
            return;
        }
        int rows = Math.max(1, ceil(height));
        for (int row = 0; row < rows; row++) {
            float progress = rows == 1 ? 0 : (float) row / (rows - 1);
            Color left = Color.interpolate(progress, ctl, cbl);
            Color right = Color.interpolate(progress, ctr, cbr);
            this.fill(x, y + row, width, 1, Color.interpolate(0.5F, left, right));
        }
    }

    private void fill(final float x, final float y, final float width, final float height, final Color color) {
        if (width <= 0 || height <= 0 || color.getAlpha() <= 0) {
            return;
        }
        this.graphics.fill((RenderPipeline) RenderPipelines.GUI, floor(x), floor(y), ceil(x + width), ceil(y + height), argb(color));
    }

    private static int addIntersection(final float[] intersections, final int count, final float x1, final float y1, final float x2, final float y2, final float y) {
        if ((y1 <= y && y2 > y) || (y2 <= y && y1 > y)) {
            intersections[count] = x1 + (y - y1) * (x2 - x1) / (y2 - y1);
            return count + 1;
        }
        return count;
    }

    private static int floor(final float value) {
        return (int) Math.floor(value);
    }

    private static int ceil(final float value) {
        return (int) Math.ceil(value);
    }

    private static int argb(final Color color) {
        return (color.getAlpha() & 0xFF) << 24
                | (color.getRed() & 0xFF) << 16
                | (color.getGreen() & 0xFF) << 8
                | (color.getBlue() & 0xFF);
    }
}
