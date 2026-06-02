package net.lenni0451.rivet.component;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.dragdrop.DragOverEvent;
import net.lenni0451.rivet.dragdrop.DropEvent;
import net.lenni0451.rivet.input.keyboard.CharEvent;
import net.lenni0451.rivet.input.keyboard.KeyEvent;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.input.mouse.MouseScrollEvent;
import net.lenni0451.rivet.layout.LayoutOptions;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;

import javax.annotation.Nullable;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

@Accessors(fluent = true, chain = true, makeFinal = true)
public abstract class Component {

    @Getter
    private Rivet rivet;
    @Getter
    @Nullable
    private Parent parent;
    @Getter
    private Size minSize = Size.EMPTY;
    @Getter
    private Size maxSize = new Size(Float.MAX_VALUE, Float.MAX_VALUE);
    @Getter
    private LayoutOptions layoutOptions;
    @Getter
    @Setter
    private boolean interactive = true;

    @Getter
    private final ListenerList<Runnable> addedListener = new ListenerList<>();
    @Getter
    private final ListenerList<Runnable> removedListener = new ListenerList<>();
    @Getter
    private final ListenerList<BooleanSupplier> focusGainedListener = new ListenerList<>();
    @Getter
    private final ListenerList<BooleanSupplier> focusLostListener = new ListenerList<>();
    @Getter
    private final ListenerList<Predicate<KeyEvent>> keyDownListener = new ListenerList<>();
    @Getter
    private final ListenerList<Predicate<KeyEvent>> keyUpListener = new ListenerList<>();
    @Getter
    private final ListenerList<Predicate<CharEvent>> charTypedListener = new ListenerList<>();
    @Getter
    private final ListenerList<BooleanSupplier> mouseEnterListener = new ListenerList<>();
    @Getter
    private final ListenerList<BooleanSupplier> mouseLeaveListener = new ListenerList<>();
    @Getter
    private final ListenerList<BiPredicate<MouseButtonEvent, Rectangle>> mouseDownListener = new ListenerList<>();
    @Getter
    private final ListenerList<BiPredicate<MouseButtonEvent, Rectangle>> mouseUpListener = new ListenerList<>();
    @Getter
    private final ListenerList<BiPredicate<MouseMoveEvent, Rectangle>> mouseMoveListener = new ListenerList<>();
    @Getter
    private final ListenerList<BiPredicate<MouseScrollEvent, Rectangle>> mouseScrollListener = new ListenerList<>();
    @Getter
    private final ListenerList<BiPredicate<DropEvent, Rectangle>> dropListener = new ListenerList<>();
    @Getter
    private final ListenerList<BiPredicate<DragOverEvent, Rectangle>> dragOverListener = new ListenerList<>();
    @Getter
    private final ListenerList<BooleanSupplier> dragLeaveListener = new ListenerList<>();

    public final void setRivet(@Nullable final Rivet rivet, @Nullable final Parent parent) {
        if (rivet == null) {
            if (parent != null) {
                throw new IllegalArgumentException("Parent must be null when detaching from Rivet");
            }
            if (this.rivet == null) {
                throw new IllegalStateException("Component is not attached to any Rivet instance");
            }
            this.removedListener.callVoid(Runnable::run, this::onComponentRemoved);
            this.rivet = null;
            this.parent = null;
        } else {
            if (parent == null) {
                throw new IllegalArgumentException("Parent must not be null when attaching to Rivet");
            }
            if (this.rivet != null) {
                throw new IllegalStateException("Component is already attached to a Rivet instance");
            }
            this.rivet = rivet;
            this.parent = parent;
            this.addedListener.callVoid(Runnable::run, this::onComponentAdded);
        }
    }

    public final Component minSize(final Size minSize) {
        if (!this.minSize.equals(minSize)) {
            this.minSize = minSize;
            if (this.parent != null) this.parent.requestLayoutRecalculation();
        }
        return this;
    }

    public final Component maxSize(final Size maxSize) {
        if (!this.maxSize.equals(maxSize)) {
            this.maxSize = maxSize;
            if (this.parent != null) this.parent.requestLayoutRecalculation();
        }
        return this;
    }

    public final Component fixedSize(final Size size) {
        Size newMinSize = this.minSize;
        Size newMaxSize = this.maxSize;
        if (size.width() >= 0) {
            newMinSize = newMinSize.withWidth(size.width());
            newMaxSize = newMaxSize.withWidth(size.width());
        }
        if (size.height() >= 0) {
            newMinSize = newMinSize.withHeight(size.height());
            newMaxSize = newMaxSize.withHeight(size.height());
        }
        if (!this.minSize.equals(newMinSize) || !this.maxSize.equals(newMaxSize)) {
            this.minSize = newMinSize;
            this.maxSize = newMaxSize;
            if (this.parent != null) this.parent.requestLayoutRecalculation();
        }
        return this;
    }

    public final Component layoutOptions(final LayoutOptions layoutOptions) {
        if (this.layoutOptions == null || !this.layoutOptions.equals(layoutOptions)) {
            this.layoutOptions = layoutOptions;
            if (this.parent != null) this.parent.requestLayoutRecalculation();
        }
        return this;
    }


    protected void onComponentAdded() {
    }

    protected void onComponentRemoved() {
    }

    public final void onFocusGained() {
        this.focusGainedListener.call(BooleanSupplier::getAsBoolean, () -> {
            this.onComponentFocusGained();
            return false;
        });
    }

    protected void onComponentFocusGained() {
    }

    public final void onFocusLost() {
        this.focusLostListener.call(BooleanSupplier::getAsBoolean, () -> {
            this.onComponentFocusLost();
            return false;
        });
    }

    protected void onComponentFocusLost() {
    }

    public final boolean onKeyDown(final KeyEvent event) {
        return this.keyDownListener.call(l -> l.test(event), () -> this.onComponentKeyDown(event));
    }

    protected boolean onComponentKeyDown(final KeyEvent event) {
        return false;
    }

    public final boolean onKeyUp(final KeyEvent event) {
        return this.keyUpListener.call(l -> l.test(event), () -> this.onComponentKeyUp(event));
    }

    protected boolean onComponentKeyUp(final KeyEvent event) {
        return false;
    }

    public final boolean onCharTyped(final CharEvent event) {
        return this.charTypedListener.call(l -> l.test(event), () -> this.onComponentCharTyped(event));
    }

    protected boolean onComponentCharTyped(final CharEvent event) {
        return false;
    }

    public final void onMouseEnter() {
        this.mouseEnterListener.call(BooleanSupplier::getAsBoolean, () -> {
            this.onComponentMouseEnter();
            return false;
        });
    }

    protected void onComponentMouseEnter() {
    }

    public final void onMouseLeave() {
        this.mouseLeaveListener.call(BooleanSupplier::getAsBoolean, () -> {
            this.onComponentMouseLeave();
            return false;
        });
    }

    protected void onComponentMouseLeave() {
    }

    public final boolean onMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        return this.mouseDownListener.call(l -> l.test(event, bounds), () -> this.onComponentMouseDown(event, bounds));
    }

    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        return false;
    }

    public final boolean onMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
        return this.mouseUpListener.call(l -> l.test(event, bounds), () -> this.onComponentMouseUp(event, bounds));
    }

    protected boolean onComponentMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
        return false;
    }

    public final boolean onMouseMove(final MouseMoveEvent event, final Rectangle bounds) {
        return this.mouseMoveListener.call(l -> l.test(event, bounds), () -> this.onComponentMouseMove(event, bounds));
    }

    protected boolean onComponentMouseMove(final MouseMoveEvent event, final Rectangle bounds) {
        return false;
    }

    public final boolean onMouseScroll(final MouseScrollEvent event, final Rectangle bounds) {
        return this.mouseScrollListener.call(l -> l.test(event, bounds), () -> this.onComponentMouseScroll(event, bounds));
    }

    protected boolean onComponentMouseScroll(final MouseScrollEvent event, final Rectangle bounds) {
        return false;
    }

    public final boolean onDrop(final DropEvent event, final Rectangle bounds) {
        return this.dropListener.call(l -> l.test(event, bounds), () -> this.onComponentDrop(event, bounds));
    }

    protected boolean onComponentDrop(final DropEvent event, final Rectangle bounds) {
        return false;
    }

    public final boolean onDragOver(final DragOverEvent event, final Rectangle bounds) {
        return this.dragOverListener.call(l -> l.test(event, bounds), () -> this.onComponentDragOver(event, bounds));
    }

    protected boolean onComponentDragOver(final DragOverEvent event, final Rectangle bounds) {
        return false;
    }

    public final void onDragLeave() {
        this.dragLeaveListener.call(BooleanSupplier::getAsBoolean, () -> {
            this.onComponentDragLeave();
            return false;
        });
    }

    protected void onComponentDragLeave() {
    }

    public void render(final Renderer renderer, final Rectangle bounds) {
    }

    public abstract Size computeIdealSize(final Size constraints);

    public void computeLayout(final Size size) {
    }

}
