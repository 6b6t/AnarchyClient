package net.lenni0451.rivet.layout;

import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import java.util.Collection;
import java.util.function.BiConsumer;

public interface Layout {

    Size computeIdealSize(final Size constraints, final Collection<Component> components);

    void layoutComponents(final Size containerSize, final Collection<Component> components, final BiConsumer<Component, Rectangle> setBounds);

    default float widthOf(final Component component, final Size idealSize) {
        return this.widthOf(component, idealSize.width());
    }

    default float widthOf(final Component component, final float width) {
        return MathUtils.clamp(width, component.minSize().width(), component.maxSize().width());
    }

    default float heightOf(final Component component, final Size idealSize) {
        return this.heightOf(component, idealSize.height());
    }

    default float heightOf(final Component component, final float height) {
        return MathUtils.clamp(height, component.minSize().height(), component.maxSize().height());
    }

}
