package net.blockhost.anarchyclient.ui;

import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.Parent;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import java.util.List;

final class LayoutDebugScrollContainer extends ScrollContainer implements LayoutDebugChildren {

    private final Component child;

    LayoutDebugScrollContainer(final Component child) {
        super(child);
        this.child = child;
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        // ScrollContainer reports its content's full ideal size, which makes surrounding grids
        // grow the scroll row and squeeze fixed rows (like the search bar) once the content gets
        // long. A scroll viewport has no intrinsic size: it always stretches via its weighted
        // cell, so report none.
        return Size.EMPTY;
    }

    @Override
    public List<LayoutDebugChild> layoutDebugChildren(final Rectangle bounds) {
        Rectangle childBounds = new Rectangle(
                scrollOffset(this.scrollX()),
                scrollOffset(this.scrollY()),
                this.debugChildSize(bounds.size())
        );
        return List.of(new LayoutDebugChild("content", this.child, childBounds, true));
    }

    private Size debugChildSize(final Size viewportSize) {
        Size contentSize = this.debugContentSize(viewportSize);
        return new Size(
                this.horizontalScrolling()
                        ? clamp(Math.max(viewportSize.width(), contentSize.width()), this.child.minSize().width(), this.child.maxSize().width())
                        : clamp(viewportSize.width(), this.child.minSize().width(), this.child.maxSize().width()),
                this.verticalScrolling()
                        ? clamp(Math.max(viewportSize.height(), contentSize.height()), this.child.minSize().height(), this.child.maxSize().height())
                        : clamp(viewportSize.height(), this.child.minSize().height(), this.child.maxSize().height())
        );
    }

    private Size debugContentSize(final Size viewportSize) {
        if (this.child instanceof Parent parent && !parent.contentSize().equals(Size.EMPTY)) {
            return parent.contentSize();
        }
        return this.child.computeIdealSize(new Size(
                this.horizontalScrolling() ? Float.MAX_VALUE : viewportSize.width(),
                this.verticalScrolling() ? Float.MAX_VALUE : viewportSize.height()
        ));
    }

    private static float scrollOffset(final float scroll) {
        return scroll == 0F ? 0F : -scroll;
    }

    private static float clamp(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }
}
