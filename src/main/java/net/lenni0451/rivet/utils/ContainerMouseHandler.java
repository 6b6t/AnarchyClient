package net.lenni0451.rivet.utils;

import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ContainerMouseHandler<C> {

    private C hoveredComponent;
    private C clickedComponent;
    private C hoveredDragComponent;
    private final Set<MouseButton> componentMouseButtons = EnumSet.noneOf(MouseButton.class);
    private final Set<MouseButton> nonComponentMouseButtons = EnumSet.noneOf(MouseButton.class);

    public boolean isMouseHeld() {
        return !this.componentMouseButtons.isEmpty() || !this.nonComponentMouseButtons.isEmpty();
    }

    public void checkAndRemove(final C component, final Consumer<C> componentMouseLeave, final BiConsumer<C, MouseButton> componentMouseUp, final Consumer<C> componentDragLeave) {
        if (component == null) return;
        if (this.hoveredComponent == component) {
            componentMouseLeave.accept(this.hoveredComponent);
            this.hoveredComponent = null;
        }
        if (this.clickedComponent == component) {
            for (MouseButton mouseButton : this.componentMouseButtons) {
                componentMouseUp.accept(this.clickedComponent, mouseButton);
            }
            this.clickedComponent = null;
            this.componentMouseButtons.clear();
        }
        if (this.hoveredDragComponent == component) {
            componentDragLeave.accept(this.hoveredDragComponent);
            this.hoveredDragComponent = null;
        }
    }

    public void clear(final Consumer<C> componentMouseLeave, final BiConsumer<C, MouseButton> componentMouseUp, final Consumer<C> componentDragLeave) {
        this.checkAndRemove(this.hoveredComponent, componentMouseLeave, componentMouseUp, componentDragLeave);
        this.checkAndRemove(this.clickedComponent, componentMouseLeave, componentMouseUp, componentDragLeave);
        this.checkAndRemove(this.hoveredDragComponent, componentMouseLeave, componentMouseUp, componentDragLeave);
    }


    public void onMouseLeave(final Consumer<C> mouseLeaveInvoker) {
        if (this.hoveredComponent != null) {
            mouseLeaveInvoker.accept(this.hoveredComponent);
            this.hoveredComponent = null;
        }
    }

    public boolean onMouseDown(final MouseButtonEvent event, @Nullable final C hoveredComponent, final Consumer<C> focusComponent, final Predicate<C> componentMouseDown, final BooleanSupplier containerMouseDown) {
        if (this.nonComponentMouseButtons.isEmpty() && hoveredComponent != null) {
            if (this.clickedComponent == null || this.clickedComponent == hoveredComponent) {
                this.clickedComponent = hoveredComponent;
                this.componentMouseButtons.add(event.button());
                focusComponent.accept(hoveredComponent);
                return componentMouseDown.test(hoveredComponent);
            }
        } else {
            this.nonComponentMouseButtons.add(event.button());
        }
        return containerMouseDown.getAsBoolean();
    }

    public boolean onMouseUp(final Rivet rivet, final MouseButtonEvent event, final Predicate<C> componentMouseUp, final BooleanSupplier containerMouseUp) {
        this.nonComponentMouseButtons.remove(event.button());
        if (this.componentMouseButtons.remove(event.button())) {
            try {
                return componentMouseUp.test(this.clickedComponent);
            } finally {
                if (this.componentMouseButtons.isEmpty()) {
                    this.clickedComponent = null;
                    rivet.updateMouseState();
                }
            }
        }
        return containerMouseUp.getAsBoolean();
    }

    public boolean onMouseMove(@Nullable final C hoveredComponent, final Consumer<C> componentMouseEnter, final Consumer<C> componentMouseLeave, final Predicate<C> componentMouseMove, final BooleanSupplier containerMouseMove) {
        if (this.hoveredComponent != null && (this.hoveredComponent != hoveredComponent || !this.nonComponentMouseButtons.isEmpty())) {
            componentMouseLeave.accept(this.hoveredComponent);
            this.hoveredComponent = null;
        }
        if (this.hoveredComponent == null && hoveredComponent != null && this.nonComponentMouseButtons.isEmpty() && (this.clickedComponent == null || this.clickedComponent == hoveredComponent)) {
            this.hoveredComponent = hoveredComponent;
            componentMouseEnter.accept(this.hoveredComponent);
        }
        if (this.clickedComponent != null) {
            return componentMouseMove.test(this.clickedComponent);
        } else if (this.hoveredComponent != null) {
            return componentMouseMove.test(this.hoveredComponent);
        } else {
            return containerMouseMove.getAsBoolean();
        }
    }

    public boolean onMouseScroll(@Nullable final C hoveredComponent, final Predicate<C> componentMouseScroll, final BooleanSupplier containerMouseScroll) {
        if (hoveredComponent != null) {
            return componentMouseScroll.test(hoveredComponent);
        } else {
            return containerMouseScroll.getAsBoolean();
        }
    }

    public boolean onDragOver(@Nullable final C hoveredComponent, final Consumer<C> componentDragLeave, final Predicate<C> componentDragOver, final BooleanSupplier containerDragOver) {
        if (this.hoveredDragComponent != null && this.hoveredDragComponent != hoveredComponent) {
            componentDragLeave.accept(this.hoveredDragComponent);
            this.hoveredDragComponent = null;
        }
        if (hoveredComponent != null) {
            this.hoveredDragComponent = hoveredComponent;
            return componentDragOver.test(hoveredComponent);
        } else {
            return containerDragOver.getAsBoolean();
        }
    }

    public void onDragLeave(final Consumer<C> componentDragLeave) {
        if (this.hoveredDragComponent != null) {
            componentDragLeave.accept(this.hoveredDragComponent);
            this.hoveredDragComponent = null;
        }
    }

}
