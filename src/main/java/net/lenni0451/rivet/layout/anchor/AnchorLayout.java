package net.lenni0451.rivet.layout.anchor;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.layout.Layout;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import java.util.Collection;
import java.util.function.BiConsumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnchorLayout implements Layout {

    public static final AnchorLayout INSTANCE = new AnchorLayout();

    @Override
    public Size computeIdealSize(final Size constraints, final Collection<Component> components) {
        Size size = Size.EMPTY;
        for (Component component : components) {
            Size idealSize = component.computeIdealSize(constraints);
            size = size.max(
                    this.widthOf(component, idealSize),
                    this.heightOf(component, idealSize)
            );
        }
        return size;
    }

    @Override
    public void layoutComponents(final Size containerSize, final Collection<Component> components, final BiConsumer<Component, Rectangle> setBounds) {
        for (Component component : components) {
            AnchorLayoutOptions options = AnchorLayoutOptions.EMPTY;
            if (component.layoutOptions() instanceof AnchorLayoutOptions o) {
                options = o;
            }

            float minX = containerSize.width() * options.anchorMinX();
            float maxX = containerSize.width() * options.anchorMaxX();
            float minY = containerSize.height() * options.anchorMinY();
            float maxY = containerSize.height() * options.anchorMaxY();
            boolean isPointAnchorX = minX >= maxX;
            boolean isPointAnchorY = minY >= maxY;
            Size idealSize = null;
            if (isPointAnchorX || isPointAnchorY) {
                idealSize = component.computeIdealSize(containerSize);
            }

            float width;
            float x;
            if (isPointAnchorX) {
                width = this.widthOf(component, idealSize.width());
                x = minX + options.offsetLeft() - options.offsetRight() - (width * options.pivotX());
            } else {
                width = this.widthOf(component, maxX - minX - options.offsetLeft() - options.offsetRight());
                x = minX + options.offsetLeft();
            }

            float height;
            float y;
            if (isPointAnchorY) {
                height = this.heightOf(component, idealSize.height());
                y = minY + options.offsetTop() - options.offsetBottom() - (height * options.pivotY());
            } else {
                height = this.heightOf(component, maxY - minY - options.offsetTop() - options.offsetBottom());
                y = minY + options.offsetTop();
            }

            setBounds.accept(component, new Rectangle(x, y, width, height));
        }
    }

}
