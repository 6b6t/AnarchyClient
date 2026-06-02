package net.lenni0451.rivet.component.container;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.lenni0451.commons.animation.Animation;
import net.lenni0451.commons.animation.AnimationDirection;
import net.lenni0451.commons.animation.EasingBehavior;
import net.lenni0451.commons.animation.easing.EasingFunction;
import net.lenni0451.commons.animation.easing.EasingMode;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.ListenerList;
import net.lenni0451.rivet.component.Parent;
import net.lenni0451.rivet.component.impl.Label;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.theme.Theme;
import net.lenni0451.rivet.theme.ThemeOption;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

@Accessors(fluent = true, chain = true, makeFinal = true)
public class Button extends Component implements Parent {

    @Getter
    private final Component child;
    @Getter
    private final ListenerList<ClickListener> clickListener;
    @Getter
    private final ThemeOption<Float> cornerRadius;
    @Getter
    private final ThemeOption<Float> outlineWidth;
    @Getter
    private final ThemeOption<Color> inactiveColor;
    @Getter
    private final ThemeOption<Color> inactiveOutlineColor;
    @Getter
    private final ThemeOption<Color> activeColor;
    @Getter
    private final ThemeOption<Color> activeOutlineColor;
    @Getter
    private final ThemeOption<Color> clickColor;
    @Getter
    private final ThemeOption<Color> clickOutlineColor;
    @Getter
    private final ThemeOption<Integer> animationDuration;
    @Getter
    private final ThemeOption<Padding> innerPadding;
    @Getter
    private final ThemeOption<ClickOn> clickOn;
    private boolean hovered = false;
    private final Set<MouseButton> pressed = new HashSet<>();
    private Animation hoverAnimation;

    public Button(final String text, final ClickListener clickListener) {
        this(new Label(text), clickListener);
    }

    public Button(final Component child, final ClickListener clickListener) {
        this(child, c -> {}, clickListener);
    }

    public <C extends Component> Button(final C child, final Consumer<C> initializer, final ClickListener clickListener) {
        this.child = child;
        initializer.accept(child);
        this.clickListener = new ListenerList<>();
        this.clickListener.add(clickListener);

        this.cornerRadius = new ThemeOption<>(this, Theme.BUTTON_CORNER_RADIUS);
        this.outlineWidth = new ThemeOption<>(this, Theme.BUTTON_OUTLINE_WIDTH);
        this.inactiveColor = new ThemeOption<>(this, Theme.BUTTON_INACTIVE_COLOR);
        this.inactiveOutlineColor = new ThemeOption<>(this, Theme.BUTTON_INACTIVE_OUTLINE_COLOR);
        this.activeColor = new ThemeOption<>(this, Theme.BUTTON_ACTIVE_COLOR);
        this.activeOutlineColor = new ThemeOption<>(this, Theme.BUTTON_ACTIVE_OUTLINE_COLOR);
        this.clickColor = new ThemeOption<>(this, Theme.BUTTON_CLICK_COLOR);
        this.clickOutlineColor = new ThemeOption<>(this, Theme.BUTTON_CLICK_OUTLINE_COLOR);
        this.animationDuration = new ThemeOption<>(this, Theme.BUTTON_ANIMATION_DURATION);
        this.innerPadding = new ThemeOption<>(this, Theme.BUTTON_INNER_PADDING);
        this.clickOn = new ThemeOption<>(this, Theme.BUTTON_CLICK_ON);
    }

    @Override
    protected void onComponentAdded() {
        this.child.setRivet(this.rivet(), this);

        this.hoverAnimation = new Animation()
                .frame(EasingFunction.SINE, EasingMode.EASE_IN_OUT, 0, 1, this.animationDuration.value(), EasingBehavior.KEEP)
                .finish(AnimationDirection.BACKWARDS);
    }

    @Override
    protected void onComponentRemoved() {
        this.child.setRivet(null, null);
        this.hovered = false;
        this.pressed.clear();
        if (this.hoverAnimation != null) {
            this.hoverAnimation.finish(AnimationDirection.BACKWARDS);
        }
    }

    @Override
    protected void onComponentMouseEnter() {
        this.hovered = true;
        this.hoverAnimation.runInDirection(AnimationDirection.FORWARDS);
    }

    @Override
    protected void onComponentMouseLeave() {
        this.hovered = false;
        this.hoverAnimation.runInDirection(AnimationDirection.BACKWARDS);
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        this.pressed.add(event.button());
        if (this.clickOn.value().equals(ClickOn.DOWN) || this.clickOn.value().equals(ClickOn.BOTH)) {
            this.clickListener.callVoid(listener -> listener.onClick(event));
        }
        return true;
    }

    @Override
    protected boolean onComponentMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
        this.pressed.remove(event.button());
        if (this.hovered && (this.clickOn.value().equals(ClickOn.UP) || this.clickOn.value().equals(ClickOn.BOTH))) {
            this.clickListener.callVoid(listener -> listener.onClick(event));
        }
        return true;
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        float cornerRadius = Math.min(this.cornerRadius.value(), Math.min(bounds.width(), bounds.height()) / 2F);
        float outlineWidth = this.outlineWidth.value();
        float animationProgress = this.hoverAnimation.getValue();
        Color color;
        Color outlineColor;
        if (this.pressed.isEmpty()) {
            color = Color.interpolate(animationProgress, this.inactiveColor.value(), this.activeColor.value());
            outlineColor = Color.interpolate(animationProgress, this.inactiveOutlineColor.value(), this.activeOutlineColor.value());
        } else {
            color = this.clickColor.value();
            outlineColor = this.clickOutlineColor.value();
        }
        renderer.optimizedFillRoundedRect(0, 0, bounds.width(), bounds.height(), cornerRadius, color);
        if (outlineWidth > 0) {
            renderer.optimizedOutlineRoundedRect(0, 0, bounds.width(), bounds.height(), cornerRadius, outlineWidth, outlineColor);
        }

        float width = bounds.width() - this.innerPadding.value().horizontal();
        float height = bounds.height() - this.innerPadding.value().vertical();
        renderer.translate(this.innerPadding.value().left(), this.innerPadding.value().top(), () -> {
            renderer.componentBounds(0, 0, width, height, () -> {
                this.child.render(renderer, new Rectangle(
                        bounds.x() + this.innerPadding.value().left(), bounds.y() + this.innerPadding.value().top(),
                        width, height
                ));
            });
        });
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return this.child.computeIdealSize(constraints.minus(this.innerPadding.value().horizontal(), this.innerPadding.value().vertical()))
                .plus(this.innerPadding.value().horizontal(), this.innerPadding.value().vertical());
    }

    @Override
    public void computeLayout(final Size size) {
        this.child.computeLayout(size.minus(this.innerPadding.value().horizontal(), this.innerPadding.value().vertical()));
    }

    @Override
    public void requestLayoutRecalculation() {
        if (this.parent() != null) this.parent().requestLayoutRecalculation();
    }

    @Override
    public Size contentSize() {
        if (this.child instanceof Parent parent) {
            return parent.contentSize().plus(this.innerPadding.value().horizontal(), this.innerPadding.value().vertical());
        }
        return Size.EMPTY;
    }


    @FunctionalInterface
    public interface ClickListener {
        void onClick(final MouseButtonEvent event);
    }

    public enum ClickOn {
        DOWN, UP, BOTH
    }

}
