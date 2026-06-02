package net.lenni0451.rivet.dragdrop;

import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.math.Rectangle;

import javax.annotation.Nullable;
import java.util.List;

public interface DropMarkerStrategy {

    static DropMarkerStrategy horizontal(final int gap, final int markerThickness) {
        return (container, mouseX, mouseY) -> {
            List<Component> children = container.children();
            for (int i = 0; i < children.size(); i++) {
                Rectangle bounds = container.childBounds(children.get(i));
                if (bounds.contains(mouseX, mouseY)) {
                    if (mouseX < bounds.x() + bounds.width() / 2F) {
                        return new DropTarget(i, new Rectangle(bounds.x() - gap / 2F - markerThickness / 2F, bounds.y(), markerThickness, bounds.height()));
                    } else {
                        return new DropTarget(i + 1, new Rectangle(bounds.x() + bounds.width() + gap / 2F - markerThickness / 2F, bounds.y(), markerThickness, bounds.height()));
                    }
                }
            }
            return null;
        };
    }

    static DropMarkerStrategy vertical(final int gap, final int markerThickness) {
        return (container, mouseX, mouseY) -> {
            List<Component> children = container.children();
            for (int i = 0; i < children.size(); i++) {
                Rectangle bounds = container.childBounds(children.get(i));
                if (bounds.contains(mouseX, mouseY)) {
                    if (mouseY < bounds.y() + bounds.height() / 2F) {
                        return new DropTarget(i, new Rectangle(bounds.x(), bounds.y() - gap / 2F - markerThickness / 2F, bounds.height(), markerThickness));
                    } else {
                        return new DropTarget(i + 1, new Rectangle(bounds.x(), bounds.y() + bounds.height() + gap / 2F - markerThickness / 2F, bounds.height(), markerThickness));
                    }
                }
            }
            return null;
        };
    }


    @Nullable
    DropTarget resolve(final Container container, final float mouseX, final float mouseY);


    record DropTarget(int insertIndex, Rectangle markerBounds) {
    }

}
