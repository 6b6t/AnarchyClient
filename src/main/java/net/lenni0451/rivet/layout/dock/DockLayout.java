package net.lenni0451.rivet.layout.dock;

import lombok.With;
import lombok.experimental.WithBy;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.layout.Layout;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import java.util.Collection;
import java.util.function.BiConsumer;

@With
@WithBy
public record DockLayout(float gap) implements Layout {

    public DockLayout() {
        this(0);
    }

    @Override
    public Size computeIdealSize(final Size constraints, final Collection<Component> components) {
        float xOffset = 0;
        float yOffset = 0;
        float totalWidth = 0;
        float totalHeight = 0;
        for (Component component : components) {
            float remainingWidth = Math.max(0, constraints.width() - xOffset);
            float remainingHeight = Math.max(0, constraints.height() - yOffset);

            DockPosition position = component.layoutOptions() instanceof DockPosition pos ? pos : DockPosition.CENTER;
            Size idealSize = component.computeIdealSize(new Size(remainingWidth, remainingHeight));
            float width = this.widthOf(component, idealSize);
            float height = this.heightOf(component, idealSize);
            switch (position) {
                case LEFT, RIGHT -> {
                    xOffset += width + this.gap;
                    totalWidth = Math.max(totalWidth, xOffset - this.gap);
                    totalHeight = Math.max(totalHeight, yOffset + height);
                }
                case TOP, BOTTOM -> {
                    yOffset += height + this.gap;
                    totalWidth = Math.max(totalWidth, xOffset + width);
                    totalHeight = Math.max(totalHeight, yOffset - this.gap);
                }
                case CENTER -> {
                    totalWidth = Math.max(totalWidth, xOffset + width);
                    totalHeight = Math.max(totalHeight, yOffset + height);
                }
            }
        }
        return new Size(totalWidth, totalHeight);
    }

    @Override
    public void layoutComponents(final Size containerSize, final Collection<Component> components, final BiConsumer<Component, Rectangle> setBounds) {
        float left = 0;
        float top = 0;
        float right = containerSize.width();
        float bottom = containerSize.height();
        for (Component component : components) {
            DockPosition position = component.layoutOptions() instanceof DockPosition pos ? pos : DockPosition.CENTER;
            float remainingWidth = Math.max(0, right - left);
            float remainingHeight = Math.max(0, bottom - top);
            Size idealSize = component.computeIdealSize(new Size(remainingWidth, remainingHeight));
            switch (position) {
                case LEFT -> {
                    float width = Math.min(remainingWidth, this.widthOf(component, idealSize.width()));
                    setBounds.accept(component, new Rectangle(left, top, width, remainingHeight));
                    left += width + this.gap;
                }
                case RIGHT -> {
                    float width = Math.min(remainingWidth, this.widthOf(component, idealSize.width()));
                    setBounds.accept(component, new Rectangle(right - width, top, width, remainingHeight));
                    right -= width + this.gap;
                }
                case TOP -> {
                    float height = Math.min(remainingHeight, this.heightOf(component, idealSize.height()));
                    setBounds.accept(component, new Rectangle(left, top, remainingWidth, height));
                    top += height + this.gap;
                }
                case BOTTOM -> {
                    float height = Math.min(remainingHeight, this.heightOf(component, idealSize.height()));
                    setBounds.accept(component, new Rectangle(left, bottom - height, remainingWidth, height));
                    bottom -= height + this.gap;
                }
                case CENTER -> {
                    setBounds.accept(component, new Rectangle(left, top, remainingWidth, remainingHeight));
                    left = right;
                    top = bottom;
                }
            }
        }
    }

}
