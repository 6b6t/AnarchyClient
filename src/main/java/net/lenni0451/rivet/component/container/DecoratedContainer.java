package net.lenni0451.rivet.component.container;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.Parent;
import net.lenni0451.rivet.dragdrop.DragOverEvent;
import net.lenni0451.rivet.dragdrop.DropEvent;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.input.mouse.MouseScrollEvent;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.utils.ContainerMouseHandler;

import java.util.function.Consumer;

@Accessors(fluent = true, chain = true, makeFinal = true)
public class DecoratedContainer extends Component implements Parent {

    @Getter
    private final Component background;
    @Getter
    private final Component child;
    private final ContainerMouseHandler<Component> mouseHandler = new ContainerMouseHandler<>();
    @Getter
    @Setter
    private Padding innerPadding;

    public DecoratedContainer(final Component background, final Component child) {
        this(background, c -> {}, child, c -> {});
    }

    public <B extends Component, C extends Component> DecoratedContainer(final B background, final Consumer<B> backgroundInitializer, final C child, final Consumer<C> childInitializer) {
        this.background = background;
        backgroundInitializer.accept(background);
        this.child = child;
        childInitializer.accept(child);

        this.innerPadding = Padding.EMPTY;
    }

    @Override
    protected void onComponentAdded() {
        this.background.setRivet(this.rivet(), this);
        this.child.setRivet(this.rivet(), this);
    }

    @Override
    protected void onComponentRemoved() {
        this.background.setRivet(null, null);
        this.child.setRivet(null, null);
    }

    @Override
    protected void onComponentMouseLeave() {
        this.mouseHandler.onMouseLeave(Component::onMouseLeave);
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        Rectangle childBounds = bounds.offset(this.innerPadding);
        boolean mouseOverChild = this.child.interactive() && childBounds.withX(this.innerPadding.left()).withY(this.innerPadding.top()).contains(event.x(), event.y());
        return this.mouseHandler.onMouseDown(
                event,
                mouseOverChild ? this.child : null,
                this.rivet()::focusedComponent,
                component -> component.onMouseDown(
                        event.withX(event.x() - this.innerPadding.left()).withY(event.y() - this.innerPadding.top()),
                        childBounds
                ),
                () -> {
                    this.rivet().focusedComponent(this.background);
                    return this.background.onMouseDown(event, bounds);
                }
        );
    }

    @Override
    protected boolean onComponentMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
        return this.mouseHandler.onMouseUp(
                this.rivet(),
                event,
                component -> {
                    Rectangle childBounds = bounds.offset(this.innerPadding);
                    return component.onMouseUp(event.withX(event.x() - this.innerPadding.left()).withY(event.y() - this.innerPadding.top()), childBounds);
                },
                () -> this.background.onMouseUp(event, bounds)
        );
    }

    @Override
    protected boolean onComponentMouseMove(final MouseMoveEvent event, final Rectangle bounds) {
        Rectangle childBounds = bounds.offset(this.innerPadding);
        boolean mouseOverChild = this.child.interactive() && childBounds.withX(this.innerPadding.left()).withY(this.innerPadding.top()).contains(event.x(), event.y());
        return this.mouseHandler.onMouseMove(
                mouseOverChild ? this.child : null,
                Component::onMouseEnter,
                Component::onMouseLeave,
                component -> component.onMouseMove(
                        event.withX(event.x() - this.innerPadding.left()).withY(event.y() - this.innerPadding.top()),
                        childBounds
                ),
                () -> this.background.onMouseMove(event, bounds)
        );
    }

    @Override
    protected boolean onComponentMouseScroll(final MouseScrollEvent event, final Rectangle bounds) {
        Rectangle childBounds = bounds.offset(this.innerPadding);
        boolean mouseOverChild = this.child.interactive() && childBounds.withX(this.innerPadding.left()).withY(this.innerPadding.top()).contains(event.x(), event.y());
        return this.mouseHandler.onMouseScroll(
                mouseOverChild ? this.child : null,
                component -> component.onMouseScroll(
                        event.withX(event.x() - this.innerPadding.left()).withY(event.y() - this.innerPadding.top()),
                        childBounds
                ),
                () -> this.background.onMouseScroll(event, bounds)
        );
    }

    @Override
    protected boolean onComponentDrop(final DropEvent event, final Rectangle bounds) {
        Rectangle childBounds = bounds.offset(this.innerPadding);
        boolean mouseOverChild = this.child.interactive() && childBounds.withX(this.innerPadding.left()).withY(this.innerPadding.top()).contains(event.x(), event.y());
        if (mouseOverChild) {
            return this.child.onDrop(event.withX(event.x() - this.innerPadding.left()).withY(event.y() - this.innerPadding.top()), childBounds);
        } else {
            return this.background.onDrop(event, childBounds);
        }
    }

    @Override
    protected boolean onComponentDragOver(final DragOverEvent event, final Rectangle bounds) {
        Rectangle childBounds = bounds.offset(this.innerPadding);
        boolean mouseOverChild = this.child.interactive() && childBounds.withX(this.innerPadding.left()).withY(this.innerPadding.top()).contains(event.x(), event.y());
        return this.mouseHandler.onDragOver(
                mouseOverChild ? this.child : null,
                Component::onDragLeave,
                component -> component.onDragOver(
                        event.withX(event.x() - this.innerPadding.left()).withY(event.y() - this.innerPadding.top()),
                        childBounds
                ),
                () -> this.background.onDragOver(event, bounds)
        );
    }

    @Override
    protected void onComponentDragLeave() {
        this.mouseHandler.onDragLeave(Component::onDragLeave);
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        this.background.render(renderer, bounds);
        float width = bounds.width() - this.innerPadding.horizontal();
        float height = bounds.height() - this.innerPadding.vertical();
        renderer.translate(this.innerPadding.left(), this.innerPadding.top(), () -> {
            renderer.componentBounds(0, 0, width, height, () -> {
                this.child.render(renderer, new Rectangle(
                        bounds.x() + this.innerPadding.left(), bounds.y() + this.innerPadding.top(),
                        width, height
                ));
            });
        });
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return this.child.computeIdealSize(constraints.minus(this.innerPadding.horizontal(), this.innerPadding.vertical()))
                .plus(this.innerPadding.horizontal(), this.innerPadding.vertical());
    }

    @Override
    public void computeLayout(final Size size) {
        this.child.computeLayout(size.minus(this.innerPadding.horizontal(), this.innerPadding.vertical()));
    }

    @Override
    public void requestLayoutRecalculation() {
        if (this.parent() != null) this.parent().requestLayoutRecalculation();
    }

    @Override
    public Size contentSize() {
        if (this.child instanceof Parent parent) {
            return parent.contentSize().plus(this.innerPadding.horizontal(), this.innerPadding.vertical());
        }
        return Size.EMPTY;
    }

}
