package net.lenni0451.rivet.layout.flow;

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
public record HorizontalFlowLayout(int horizontalGap, int verticalGap) implements Layout {

    public HorizontalFlowLayout() {
        this(0, 0);
    }

    @Override
    public Size computeIdealSize(final Size constraints, final Collection<Component> components) {
        float currentWidth = 0;
        float currentHeight = 0;
        float totalWidth = 0;
        float totalHeight = 0;
        for (Component component : components) {
            Size idealSize = component.computeIdealSize(constraints);
            float xGap = currentWidth > 0 ? this.horizontalGap : 0;
            float componentWidth = this.widthOf(component, idealSize);
            float componentHeight = this.heightOf(component, idealSize);
            if (currentWidth > 0 && currentWidth + xGap + componentWidth > constraints.width()) {
                totalWidth = Math.max(totalWidth, currentWidth);
                currentWidth = 0;

                if (totalHeight > 0) totalHeight += this.verticalGap;
                totalHeight += currentHeight;
                currentHeight = 0;
            }
            currentWidth += xGap + componentWidth;
            currentHeight = Math.max(currentHeight, componentHeight);
        }
        if (currentWidth > 0) {
            totalWidth = Math.max(totalWidth, currentWidth);
            if (totalHeight > 0) totalHeight += this.verticalGap;
            totalHeight += currentHeight;
        }
        return new Size(totalWidth, totalHeight);
    }

    @Override
    public void layoutComponents(final Size containerSize, final Collection<Component> components, final BiConsumer<Component, Rectangle> setBounds) {
        float x = 0;
        float y = 0;
        float maxHeight = 0;
        for (Component component : components) {
            Size idealSize = component.computeIdealSize(containerSize);
            float width = this.widthOf(component, idealSize);
            float height = this.heightOf(component, idealSize);
            if (x + width > containerSize.width() && x > 0) {
                x = 0;
                y += maxHeight + this.verticalGap;
                maxHeight = height;
            }
            setBounds.accept(component, new Rectangle(x, y, width, height));
            x += width + this.horizontalGap;
            maxHeight = Math.max(maxHeight, height);
        }
    }

}
