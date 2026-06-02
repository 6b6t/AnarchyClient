package net.lenni0451.rivet.backend.render;

import lombok.RequiredArgsConstructor;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.Texture;
import net.lenni0451.rivet.backend.text.ShapedText;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.text.model.TextOrigin;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;

public final class Renderer {

    private final Stack<IncompleteRenderList> currentRenderList = new Stack<>();

    public Renderer() {
        this.currentRenderList.push(new IncompleteRenderList());
    }

    private void checkClosed() {
        if (this.currentRenderList.isEmpty()) {
            throw new IllegalStateException("Renderer has been closed");
        }
    }

    public RenderList complete() {
        this.checkClosed();
        if (this.currentRenderList.size() > 1) {
            throw new IllegalStateException("Not all render lists have been completed (" + this.currentRenderList.size() + ")");
        }
        return this.currentRenderList.pop().complete();
    }


    public void translate(final float x, final float y, final Runnable renderer) {
        this.transform(new TransformCommand.Translate(x, y), renderer);
    }

    public void componentBounds(final float x, final float y, final float width, final float height, final Runnable renderer) {
        this.transform(new TransformCommand.ComponentBounds(x, y, width, height), renderer);
    }

    public void scissor(final float x, final float y, final float width, final float height, final Runnable renderer) {
        this.transform(new TransformCommand.Scissor(x, y, width, height), renderer);
    }

    public void scale(final float xy, final Runnable renderer) {
        this.scale(xy, xy, renderer);
    }

    public void scale(final float x, final float y, final Runnable renderer) {
        if (x == 1 && y == 1) {
            renderer.run();
        } else {
            this.transform(new TransformCommand.Scale(x, y), renderer);
        }
    }

    private void transform(final TransformCommand command, final Runnable renderer) {
        this.checkClosed();
        this.currentRenderList.push(new IncompleteRenderList(command));
        renderer.run();
        IncompleteRenderList subList = this.currentRenderList.pop();
        this.currentRenderList.peek().add(subList.complete());
    }


    public void fillCircle(final float x, final float y, final float radius, final Color color) {
        this.checkClosed();
        this.currentRenderList.peek().add(new RenderCommand.FillCircle(x, y, radius, color));
    }

    public void outlineCircle(final float x, final float y, final float radius, final float outlineWidth, final Color color) {
        this.checkClosed();
        this.currentRenderList.peek().add(new RenderCommand.OutlineCircle(x, y, radius, outlineWidth, color));
    }

    public void fillTriangle(final float x1, final float y1, final float x2, final float y2, final float x3, final float y3, final Color color) {
        this.checkClosed();
        this.currentRenderList.peek().add(new RenderCommand.FillTriangle(x1, y1, x2, y2, x3, y3, color));
    }

    public void fillRect(final float x, final float y, final float width, final float height, final Color color) {
        this.checkClosed();
        this.currentRenderList.peek().add(new RenderCommand.FillRect(x, y, width, height, color));
    }

    public void outlineRect(final float x, final float y, final float width, final float height, final float outlineWidth, final Color color) {
        this.checkClosed();
        this.currentRenderList.peek().add(new RenderCommand.OutlineRect(x, y, width, height, outlineWidth, color));
    }

    public void fillRoundedRect(final float x, final float y, final float width, final float height, final float cornerRadius, final Color color) {
        this.checkClosed();
        this.currentRenderList.peek().add(new RenderCommand.FillRoundedRect(x, y, width, height, cornerRadius, color));
    }

    public void outlineRoundedRect(final float x, final float y, final float width, final float height, final float cornerRadius, final float outlineWidth, final Color color) {
        this.checkClosed();
        this.currentRenderList.peek().add(new RenderCommand.OutlineRoundedRect(x, y, width, height, cornerRadius, outlineWidth, color));
    }

    public void optimizedFillRoundedRect(final float x, final float y, final float width, final float height, final float cornerRadius, final Color color) {
        this.checkClosed();
        float maxRadius = Math.min(width, height) / 2F;
        float radius = Math.min(cornerRadius, maxRadius);
        if (radius <= 0) {
            this.fillRect(x, y, width, height, color);
        } else if (width == height && radius == maxRadius) {
            this.fillCircle(x + radius, y + radius, radius, color);
        } else {
            this.fillRoundedRect(x, y, width, height, radius, color);
        }
    }

    public void optimizedOutlineRoundedRect(final float x, final float y, final float width, final float height, final float cornerRadius, final float outlineWidth, final Color color) {
        this.checkClosed();
        float maxRadius = Math.min(width, height) / 2F;
        float radius = Math.min(cornerRadius, maxRadius);
        if (radius <= 0) {
            this.outlineRect(x, y, width, height, outlineWidth, color);
        } else if (width == height && radius == maxRadius) {
            this.outlineCircle(x + radius, y + radius, radius, outlineWidth, color);
        } else {
            this.outlineRoundedRect(x, y, width, height, radius, outlineWidth, color);
        }
    }

    public void line(final float x1, final float y1, final float x2, final float y2, final float width, final Color color) {
        this.checkClosed();
        this.currentRenderList.peek().add(new RenderCommand.Line(x1, y1, x2, y2, width, color));
    }

    public void fillGradientRect(final float x, final float y, final float width, final float height, final Color ctl, final Color cbl, final Color cbr, final Color ctr) {
        this.checkClosed();
        this.currentRenderList.peek().add(new RenderCommand.FillGradientRect(x, y, width, height, ctl, cbl, cbr, ctr));
    }

    public void text(final ShapedText shapedText, final float x, final float y, final TextOrigin.Horizontal horizontalOrigin, final TextOrigin.Vertical verticalOrigin) {
        this.checkClosed();
        float tx = x + shapedText.offset(horizontalOrigin);
        float ty = y + shapedText.offset(verticalOrigin);
        this.currentRenderList.peek().add(new RenderCommand.Text(shapedText, tx, ty));
    }

    public void image(final Texture texture, final float x, final float y, final float width, final float height, final Color color) {
        this.checkClosed();
        this.currentRenderList.peek().add(new RenderCommand.Image(texture, x, y, width, height, color));
    }

    /**
     * Push a custom render command to the backend.<br>
     * The code in the renderer is highly backend specific and is not portable.<br>
     * The backend may choose to execute the renderer in a separate thread, make sure all data passed is immutable.<br>
     * <b>The type {@code T} is not checked. Make sure it matches the backend type!</b>
     *
     * @param renderer The custom renderer
     * @param bounds   The bounds of the renderer
     * @param <T>      The backend specific type
     */
    public <T> void custom(final Consumer<T> renderer, final Rectangle bounds) {
        this.checkClosed();
        this.currentRenderList.peek().add(new RenderCommand.CustomRenderCommand<>(renderer, bounds));
    }


    @RequiredArgsConstructor
    private static class IncompleteRenderList {
        @Nullable
        private final TransformCommand transform;
        private final List<RenderElement> elements = new ArrayList<>();
        private boolean closed;

        public IncompleteRenderList() {
            this(null);
        }

        public void add(final RenderElement element) {
            this.checkClosed();
            this.elements.add(element);
        }

        public RenderList complete() {
            this.checkClosed();
            this.closed = true;
            return new RenderList(this.transform, this.elements);
        }

        private void checkClosed() {
            if (this.closed) {
                throw new IllegalStateException("Render list has been closed");
            }
        }
    }

}
