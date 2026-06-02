package net.lenni0451.rivet.layout.list;

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
public record HorizontalListLayout(int gap, boolean fullHeight) implements Layout {

    public HorizontalListLayout() {
        this(0, false);
    }

    @Override
    public Size computeIdealSize(final Size constraints, final Collection<Component> components) {
        float width = 0;
        float height = 0;
        for (Component component : components) {
            Size idealSize = component.computeIdealSize(constraints);
            width += this.widthOf(component, idealSize);
            height = Math.max(height, this.heightOf(component, idealSize));
        }
        if (!components.isEmpty()) {
            width += this.gap * (components.size() - 1);
        }
        return new Size(width, height);
    }

    @Override
    public void layoutComponents(final Size containerSize, final Collection<Component> components, final BiConsumer<Component, Rectangle> setBounds) {
        float x = 0;
        for (Component component : components) {
            Size idealSize = component.computeIdealSize(containerSize);
            float width = this.widthOf(component, idealSize);
            float height = this.fullHeight ? this.heightOf(component, containerSize.height()) : this.heightOf(component, idealSize);
            if (x > 0) x += this.gap;
            setBounds.accept(component, new Rectangle(x, 0, width, height));
            x += width;
        }
    }

}
