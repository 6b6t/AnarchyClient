package net.lenni0451.rivet.layout.border;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.layout.Layout;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import java.util.Collection;
import java.util.function.BiConsumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BorderLayout implements Layout {

    public static final BorderLayout INSTANCE = new BorderLayout();

    @Override
    public Size computeIdealSize(final Size constraints, final Collection<Component> components) {
        Size top = Size.EMPTY;
        Size bottom = Size.EMPTY;
        Size left = Size.EMPTY;
        Size right = Size.EMPTY;
        Size center = Size.EMPTY;
        for (Component component : components) {
            BorderPosition position = component.layoutOptions() instanceof BorderPosition pos ? pos : BorderPosition.CENTER;
            Size idealSize = component.computeIdealSize(constraints);
            switch (position) {
                case TOP -> top = top.max(idealSize);
                case BOTTOM -> bottom = bottom.max(idealSize);
                case LEFT -> left = left.max(idealSize);
                case RIGHT -> right = right.max(idealSize);
                case CENTER -> center = center.max(idealSize);
            }
        }
        return new Size(
                Math.max(Math.max(left.width() + center.width() + right.width(), top.width()), bottom.width()),
                Math.max(Math.max(center.height(), left.height()), right.height()) + top.height() + bottom.height()
        );
    }

    @Override
    public void layoutComponents(final Size containerSize, final Collection<Component> components, final BiConsumer<Component, Rectangle> setBounds) {
        float topHeight = 0;
        float bottomHeight = 0;
        float leftWidth = 0;
        float rightWidth = 0;
        for (Component component : components) {
            BorderPosition position = component.layoutOptions() instanceof BorderPosition pos ? pos : BorderPosition.CENTER;
            Size idealSize = component.computeIdealSize(containerSize);
            switch (position) {
                case TOP -> topHeight = Math.max(topHeight, idealSize.height());
                case BOTTOM -> bottomHeight = Math.max(bottomHeight, idealSize.height());
                case LEFT -> leftWidth = Math.max(leftWidth, idealSize.width());
                case RIGHT -> rightWidth = Math.max(rightWidth, idealSize.width());
            }
        }
        for (Component component : components) {
            BorderPosition position = component.layoutOptions() instanceof BorderPosition pos ? pos : BorderPosition.CENTER;
            setBounds.accept(component, switch (position) {
                case TOP -> new Rectangle(0, 0, containerSize.width(), topHeight);
                case BOTTOM -> new Rectangle(0, containerSize.height() - bottomHeight, containerSize.width(), bottomHeight);
                case LEFT -> new Rectangle(0, topHeight, leftWidth, containerSize.height() - topHeight - bottomHeight);
                case RIGHT -> new Rectangle(containerSize.width() - rightWidth, topHeight, rightWidth, containerSize.height() - topHeight - bottomHeight);
                case CENTER -> new Rectangle(leftWidth, topHeight, containerSize.width() - leftWidth - rightWidth, containerSize.height() - topHeight - bottomHeight);
            });
        }
    }

}
