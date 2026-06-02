package net.lenni0451.rivet.component.container;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.lenni0451.commons.animation.DynamicAnimation;
import net.lenni0451.commons.animation.easing.EasingFunction;
import net.lenni0451.commons.animation.easing.EasingMode;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.ListenerList;
import net.lenni0451.rivet.component.Parent;
import net.lenni0451.rivet.dragdrop.DragOverEvent;
import net.lenni0451.rivet.dragdrop.DropEvent;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.input.mouse.MouseScrollEvent;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.math.Snapping;
import net.lenni0451.rivet.theme.Theme;
import net.lenni0451.rivet.theme.ThemeOption;
import net.lenni0451.rivet.utils.ContainerMouseHandler;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Accessors(fluent = true, chain = true, makeFinal = true)
public class ScrollContainer extends Component implements Parent {

    private final Component child;
    @Getter
    private final boolean horizontalScrolling;
    @Getter
    private final boolean verticalScrolling;
    @Getter
    @Setter
    private boolean autoScroll;
    @Getter
    @Setter
    private float autoScrollThreshold = 0.1F;

    @Getter
    private final ThemeOption<Color> barColor;
    @Getter
    private final ThemeOption<Color> barHoverColor;
    @Getter
    private final ThemeOption<Color> barClickColor;
    @Getter
    private final ThemeOption<Float> barWidth;
    @Getter
    private final ThemeOption<Float> barCornerRadius;
    @Getter
    private final ThemeOption<Float> barOutlineWidth;
    @Getter
    private final ThemeOption<Color> barOutlineColor;
    @Getter
    private final ThemeOption<Float> scrollSpeed;
    @Getter
    private final ThemeOption<Boolean> smoothScrolling;
    @Getter
    private final ThemeOption<Integer> animationDuration;
    @Getter
    private final ThemeOption<Long> nestedScrollTimeout;
    @Getter
    private final ThemeOption<ScrollBarType> barType;
    @Getter
    private final ThemeOption<Boolean> railClickJump;
    @Getter
    private final ThemeOption<Color> railColor;
    @Getter
    private final ThemeOption<Color> railOutlineColor;
    @Getter
    private final ThemeOption<Float> railOutlineWidth;

    private Size childSize = Size.EMPTY;
    private final ContainerMouseHandler<Component> mouseHandler = new ContainerMouseHandler<>();

    private final NestedScrollCoordinator nestedScrollCoordinator = new NestedScrollCoordinator();
    @Getter
    private final ListenerList<ScrollListener> scrollListener = new ListenerList<>();
    @Getter
    private float scrollX;
    @Getter
    private float scrollY;
    private float targetScrollX;
    private float targetScrollY;
    private DynamicAnimation scrollXAnimation;
    private DynamicAnimation scrollYAnimation;

    private boolean hBarHovered;
    private boolean hBarPressed;
    private boolean hRailHovered;
    private boolean hRailPressed;
    private boolean vBarHovered;
    private boolean vBarPressed;
    private boolean vRailHovered;
    private boolean vRailPressed;
    private float dragStartX;
    private float dragStartY;
    private float initialScrollX;
    private float initialScrollY;
    private boolean vScrollVisible;
    private boolean hScrollVisible;

    public ScrollContainer(final Component child) {
        this(child, false, true);
    }

    public <C extends Component> ScrollContainer(final C child, final Consumer<C> initializer) {
        this(child, initializer, false, true);
    }

    public ScrollContainer(final Component child, final boolean horizontalScrolling, final boolean verticalScrolling) {
        this(child, c -> {}, horizontalScrolling, verticalScrolling);
    }

    public <C extends Component> ScrollContainer(final C child, final Consumer<C> initializer, final boolean horizontalScrolling, final boolean verticalScrolling) {
        this.child = child;
        initializer.accept(child);
        this.horizontalScrolling = horizontalScrolling;
        this.verticalScrolling = verticalScrolling;

        this.barColor = new ThemeOption<>(this, Theme.SCROLL_BAR_COLOR);
        this.barHoverColor = new ThemeOption<>(this, Theme.SCROLL_BAR_HOVER_COLOR);
        this.barClickColor = new ThemeOption<>(this, Theme.SCROLL_BAR_CLICK_COLOR);
        this.barWidth = new ThemeOption<>(this, Theme.SCROLL_BAR_WIDTH);
        this.barCornerRadius = new ThemeOption<>(this, Theme.SCROLL_BAR_CORNER_RADIUS);
        this.barOutlineWidth = new ThemeOption<>(this, Theme.SCROLL_BAR_OUTLINE_WIDTH);
        this.barOutlineColor = new ThemeOption<>(this, Theme.SCROLL_BAR_OUTLINE_COLOR);
        this.scrollSpeed = new ThemeOption<>(this, Theme.SCROLL_SPEED);
        this.smoothScrolling = new ThemeOption<>(this, Theme.SCROLL_SMOOTH);
        this.animationDuration = new ThemeOption<>(this, Theme.SCROLL_ANIMATION_DURATION);
        this.nestedScrollTimeout = new ThemeOption<>(this, Theme.SCROLL_NESTED_SCROLL_TIMEOUT);
        this.barType = new ThemeOption<>(this, Theme.SCROLL_BAR_TYPE);
        this.railClickJump = new ThemeOption<>(this, Theme.SCROLL_RAIL_CLICK_JUMP);
        this.railColor = new ThemeOption<>(this, Theme.SCROLL_RAIL_COLOR);
        this.railOutlineColor = new ThemeOption<>(this, Theme.SCROLL_RAIL_OUTLINE_COLOR);
        this.railOutlineWidth = new ThemeOption<>(this, Theme.SCROLL_RAIL_OUTLINE_WIDTH);
    }

    @Override
    protected void onComponentAdded() {
        this.child.setRivet(this.rivet(), this);
        this.scrollXAnimation = new DynamicAnimation(EasingFunction.SINE, EasingMode.EASE_OUT, (long) this.animationDuration.value(), 0);
        this.scrollYAnimation = new DynamicAnimation(EasingFunction.SINE, EasingMode.EASE_OUT, (long) this.animationDuration.value(), 0);
    }

    @Override
    protected void onComponentRemoved() {
        this.child.setRivet(null, null);
        this.hBarHovered = false;
        this.hBarPressed = false;
        this.hRailHovered = false;
        this.hRailPressed = false;
        this.vBarHovered = false;
        this.vBarPressed = false;
        this.vRailHovered = false;
        this.vRailPressed = false;
    }

    @Override
    protected void onComponentMouseLeave() {
        this.mouseHandler.onMouseLeave(Component::onMouseLeave);
        this.hBarHovered = false;
        this.hRailHovered = false;
        this.vBarHovered = false;
        this.vRailHovered = false;
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        Rectangle hThumb = this.getHThumbBounds(bounds);
        Rectangle hRail = this.getHRailBounds(bounds);
        Rectangle vThumb = this.getVThumbBounds(bounds);
        Rectangle vRail = this.getVRailBounds(bounds);
        boolean componentHovered = (hThumb == null || !hThumb.contains(event.x(), event.y()))
                && (hRail == null || !hRail.contains(event.x(), event.y()))
                && (vThumb == null || !vThumb.contains(event.x(), event.y()))
                && (vRail == null || !vRail.contains(event.x(), event.y()));
        return this.mouseHandler.onMouseDown(
                event,
                componentHovered ? this.child : null,
                this.rivet()::focusedComponent,
                component -> component.onMouseDown(
                        event.withX(event.x() + this.scrollX).withY(event.y() + this.scrollY),
                        new Rectangle(bounds.x() - this.scrollX, bounds.y() - this.scrollY, this.childSize)
                ),
                () -> {
                    this.rivet().focusedComponent(this);
                    if (event.button().equals(MouseButton.LEFT)) {
                        if (hThumb != null && hThumb.contains(event.x(), event.y())) {
                            this.hBarPressed = true;
                            this.dragStartX = event.x();
                            this.initialScrollX = this.targetScrollX;
                            return true;
                        } else if (hRail != null && hRail.contains(event.x(), event.y())) {
                            this.hRailPressed = true;
                            float visibleWidth = this.visibleWidth(bounds);
                            float maxScroll = Math.max(0, this.childSize.width() - visibleWidth);
                            if (this.railClickJump.value()) {
                                float thumbWidth = hThumb.width();
                                float scrollableWidth = hRail.width() - thumbWidth;
                                float clickX = event.x() - hRail.x() - thumbWidth / 2F;
                                this.targetScrollX = MathUtils.clamp((clickX / scrollableWidth) * maxScroll, 0, maxScroll);
                            } else {
                                if (event.x() < hThumb.x()) this.targetScrollX = MathUtils.clamp(this.targetScrollX - visibleWidth, 0, maxScroll);
                                else this.targetScrollX = MathUtils.clamp(this.targetScrollX + visibleWidth, 0, maxScroll);
                            }
                            return true;
                        } else if (vThumb != null && vThumb.contains(event.x(), event.y())) {
                            this.vBarPressed = true;
                            this.dragStartY = event.y();
                            this.initialScrollY = this.targetScrollY;
                            return true;
                        } else if (vRail != null && vRail.contains(event.x(), event.y())) {
                            this.vRailPressed = true;
                            float visibleHeight = this.visibleHeight(bounds);
                            float maxScroll = Math.max(0, this.childSize.height() - visibleHeight);
                            if (this.railClickJump.value()) {
                                float thumbHeight = vThumb.height();
                                float scrollableHeight = vRail.height() - thumbHeight;
                                float clickY = event.y() - vRail.y() - thumbHeight / 2F;
                                this.targetScrollY = MathUtils.clamp((clickY / scrollableHeight) * maxScroll, 0, maxScroll);
                            } else {
                                if (event.y() < vThumb.y()) this.targetScrollY = MathUtils.clamp(this.targetScrollY - visibleHeight, 0, maxScroll);
                                else this.targetScrollY = MathUtils.clamp(this.targetScrollY + visibleHeight, 0, maxScroll);
                            }
                            return true;
                        }
                    }
                    return false;
                }
        );
    }

    @Override
    protected boolean onComponentMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
        return this.mouseHandler.onMouseUp(
                this.rivet(),
                event,
                component -> component.onMouseUp(
                        event.withX(event.x() + this.scrollX).withY(event.y() + this.scrollY),
                        new Rectangle(bounds.x() - this.scrollX, bounds.y() - this.scrollY, this.childSize)
                ),
                () -> {
                    if (event.button().equals(MouseButton.LEFT)) {
                        this.hBarPressed = false;
                        this.hRailPressed = false;
                        this.vBarPressed = false;
                        this.vRailPressed = false;
                        return true;
                    }
                    return false;
                }
        );
    }

    @Override
    protected boolean onComponentMouseMove(final MouseMoveEvent event, final Rectangle bounds) {
        Rectangle hThumb = this.getHThumbBounds(bounds);
        Rectangle hRail = this.getHRailBounds(bounds);
        Rectangle vThumb = this.getVThumbBounds(bounds);
        Rectangle vRail = this.getVRailBounds(bounds);

        this.hBarHovered = hThumb != null && hThumb.contains(event.x(), event.y());
        this.hRailHovered = hRail != null && hRail.contains(event.x(), event.y());
        this.vBarHovered = vThumb != null && vThumb.contains(event.x(), event.y());
        this.vRailHovered = vRail != null && vRail.contains(event.x(), event.y());
        boolean componentHovered = !this.hBarHovered && !this.hRailHovered && !this.vBarHovered && !this.vRailHovered;

        return this.mouseHandler.onMouseMove(
                componentHovered ? this.child : null,
                Component::onMouseEnter,
                Component::onMouseLeave,
                component -> component.onMouseMove(
                        event.withX(event.x() + this.scrollX).withY(event.y() + this.scrollY),
                        new Rectangle(bounds.x() - this.scrollX, bounds.y() - this.scrollY, this.childSize)
                ),
                () -> {
                    if (this.hBarPressed && hThumb != null) {
                        float contentWidth = this.childSize.width();
                        float visibleWidth = this.visibleWidth(bounds);
                        float maxScroll = Math.max(0, contentWidth - visibleWidth);
                        Rectangle rail = this.getHRailBounds(bounds);
                        float barWidth = hThumb.width();
                        float scrollableWidth = rail.width() - barWidth;
                        float dragDelta = event.x() - this.dragStartX;
                        this.targetScrollX = MathUtils.clamp(this.initialScrollX + (dragDelta / scrollableWidth) * maxScroll, 0, maxScroll);
                        return true;
                    } else if (this.vBarPressed && vThumb != null) {
                        float contentHeight = this.childSize.height();
                        float visibleHeight = this.visibleHeight(bounds);
                        float maxScroll = Math.max(0, contentHeight - visibleHeight);
                        Rectangle rail = this.getVRailBounds(bounds);
                        float barHeight = vThumb.height();
                        float scrollableHeight = rail.height() - barHeight;
                        float dragDelta = event.y() - this.dragStartY;
                        this.targetScrollY = MathUtils.clamp(this.initialScrollY + (dragDelta / scrollableHeight) * maxScroll, 0, maxScroll);
                        return true;
                    }
                    return false;
                }
        );
    }

    @Override
    protected boolean onComponentMouseScroll(final MouseScrollEvent event, final Rectangle bounds) {
        return this.nestedScrollCoordinator.handleScrolling(
                () -> {
                    if (this.hScrollVisible && event.scrollX() != 0 && !this.hBarPressed) {
                        float contentWidth = this.childSize.width();
                        float visibleWidth = this.visibleWidth(bounds);
                        float maxScroll = Math.max(0, contentWidth - visibleWidth);
                        if (maxScroll > 0) {
                            this.targetScrollX = MathUtils.clamp(this.targetScrollX - event.scrollX() * this.scrollSpeed.value(), 0, maxScroll);
                            return true;
                        }
                    }
                    if (this.vScrollVisible && event.scrollY() != 0 && !this.vBarPressed) {
                        float contentHeight = this.childSize.height();
                        float visibleHeight = this.visibleHeight(bounds);
                        float maxScroll = Math.max(0, contentHeight - visibleHeight);
                        if (maxScroll > 0) {
                            this.targetScrollY = MathUtils.clamp(this.targetScrollY - event.scrollY() * this.scrollSpeed.value(), 0, maxScroll);
                            return true;
                        }
                    }
                    return false;
                },
                () -> this.child.onMouseScroll(
                        event.withX(event.x() + this.scrollX).withY(event.y() + this.scrollY),
                        new Rectangle(bounds.x() - this.scrollX, bounds.y() - this.scrollY, this.childSize)
                )
        );
    }

    @Override
    protected boolean onComponentDrop(final DropEvent event, final Rectangle bounds) {
        Component hoveredComponent = this.hoveredComponent(event.x(), event.y(), bounds);
        if (hoveredComponent != null) {
            return this.child.onDrop(
                    event.withX(event.x() + this.scrollX).withY(event.y() + this.scrollY),
                    new Rectangle(bounds.x() - this.scrollX, bounds.y() - this.scrollY, this.childSize)
            );
        } else {
            return false;
        }
    }

    @Override
    protected boolean onComponentDragOver(final DragOverEvent event, final Rectangle bounds) {
        return this.mouseHandler.onDragOver(
                this.hoveredComponent(event.x(), event.y(), bounds),
                Component::onDragLeave,
                component -> component.onDragOver(
                        event.withX(event.x() + this.scrollX).withY(event.y() + this.scrollY),
                        new Rectangle(bounds.x() - this.scrollX, bounds.y() - this.scrollY, this.childSize)
                ),
                () -> false
        );
    }

    @Override
    protected void onComponentDragLeave() {
        this.mouseHandler.onDragLeave(Component::onDragLeave);
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        this.updateAnimation();

        float childWidth = this.visibleWidth(bounds);
        float childHeight = this.visibleHeight(bounds);

        renderer.componentBounds(0, 0, childWidth, childHeight, () -> {
            renderer.translate(-this.scrollX, -this.scrollY, () -> {
                this.child.render(renderer, new Rectangle(bounds.x() - this.scrollX, bounds.y() - this.scrollY, this.childSize));
            });
        });

        this.renderHorizontalScrollbar(renderer, bounds);
        this.renderVerticalScrollbar(renderer, bounds);
        this.renderCorner(renderer, bounds);
    }

    @Nullable
    private Component hoveredComponent(final float x, final float y, final Rectangle bounds) {
        Rectangle hThumb = this.getHThumbBounds(bounds);
        Rectangle hRail = this.getHRailBounds(bounds);
        Rectangle vThumb = this.getVThumbBounds(bounds);
        Rectangle vRail = this.getVRailBounds(bounds);
        boolean componentHovered = (hThumb == null || !hThumb.contains(x, y))
                && (hRail == null || !hRail.contains(x, y))
                && (vThumb == null || !vThumb.contains(x, y))
                && (vRail == null || !vRail.contains(x, y));
        return componentHovered ? this.child : null;
    }

    private void updateAnimation() {
        if (this.smoothScrolling.value()) {
            this.scrollXAnimation.setTarget(this.targetScrollX);
            this.scrollYAnimation.setTarget(this.targetScrollY);
        } else {
            this.scrollXAnimation.setTarget(this.targetScrollX).finish();
            this.scrollYAnimation.setTarget(this.targetScrollY).finish();
        }
        float oldScrollX = this.scrollX;
        float oldScrollY = this.scrollY;
        this.scrollX = Snapping.snap(this.rivet(), this.scrollXAnimation.getValue());
        this.scrollY = Snapping.snap(this.rivet(), this.scrollYAnimation.getValue());
        if (oldScrollX != this.scrollX || oldScrollY != this.scrollY) {
            if (this.rivet() != null) {
                this.rivet().updateMouseState();
            }
            this.onScroll();
        }
    }

    private void renderHorizontalScrollbar(final Renderer renderer, final Rectangle bounds) {
        Rectangle rail = this.getHRailBounds(bounds);
        if (rail == null) return;
        if (this.barType.value() == ScrollBarType.NORMAL) {
            this.renderRail(renderer, rail, this.hRailHovered, this.hRailPressed);
        }
        this.renderThumb(renderer, this.getHThumbBounds(bounds), this.hBarHovered, this.hBarPressed);
    }

    private void renderVerticalScrollbar(final Renderer renderer, final Rectangle bounds) {
        Rectangle rail = this.getVRailBounds(bounds);
        if (rail == null) return;
        if (this.barType.value() == ScrollBarType.NORMAL) {
            this.renderRail(renderer, rail, this.vRailHovered, this.vRailPressed);
        }
        this.renderThumb(renderer, this.getVThumbBounds(bounds), this.vBarHovered, this.vBarPressed);
    }

    private void renderCorner(final Renderer renderer, final Rectangle bounds) {
        if (this.barType.value() == ScrollBarType.NORMAL && this.vScrollVisible && this.hScrollVisible) {
            float barWidth = this.barWidth.value();
            renderer.fillRect(bounds.width() - barWidth, bounds.height() - barWidth, barWidth, barWidth, this.railColor.value());
        }
    }

    private void renderRail(final Renderer renderer, final Rectangle bounds, final boolean hovered, final boolean pressed) {
        renderer.fillRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), this.railColor.value());
        if (this.railOutlineWidth.value() > 0) {
            renderer.outlineRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), this.railOutlineWidth.value(), this.railOutlineColor.value());
        }
    }

    private void renderThumb(final Renderer renderer, final Rectangle bounds, final boolean hovered, final boolean pressed) {
        if (bounds == null) return;

        Color color = this.barColor.value();
        if (pressed) color = this.barClickColor.value();
        else if (hovered) color = this.barHoverColor.value();

        renderer.optimizedFillRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), this.barCornerRadius.value(), color);
        if (this.barOutlineWidth.value() > 0) {
            renderer.optimizedOutlineRoundedRect(bounds.x(), bounds.y(), bounds.width(), bounds.height(), this.barCornerRadius.value(), this.barOutlineWidth.value(), this.barOutlineColor.value());
        }
    }

    private float visibleWidth(final Rectangle bounds) {
        return bounds.width() - (this.vScrollVisible && this.barType.value() == ScrollBarType.NORMAL ? this.barWidth.value() : 0);
    }

    private float visibleHeight(final Rectangle bounds) {
        return bounds.height() - (this.hScrollVisible && this.barType.value() == ScrollBarType.NORMAL ? this.barWidth.value() : 0);
    }

    @Nullable
    private Rectangle getHScrollArea(final Rectangle bounds) {
        if (!this.hScrollVisible) return null;
        float barWidth = this.barWidth.value();
        return new Rectangle(0, bounds.height() - barWidth, bounds.width() - (this.vScrollVisible && this.barType.value() == ScrollBarType.NORMAL ? barWidth : 0), barWidth);
    }

    @Nullable
    private Rectangle getHRailBounds(final Rectangle bounds) {
        return this.getHScrollArea(bounds);
    }

    @Nullable
    private Rectangle getHThumbBounds(final Rectangle bounds) {
        Rectangle rail = this.getHRailBounds(bounds);
        if (rail == null) return null;
        float contentWidth = this.childSize.width();
        float visibleWidth = this.visibleWidth(bounds);
        float thumbWidth = Math.max(20, (visibleWidth / contentWidth) * rail.width());
        float maxScroll = Math.max(1, contentWidth - visibleWidth);
        float scrollPercentage = this.scrollX / maxScroll;
        float thumbX = rail.x() + scrollPercentage * (rail.width() - thumbWidth);
        return new Rectangle(thumbX, rail.y(), thumbWidth, rail.height());
    }

    @Nullable
    private Rectangle getVScrollArea(final Rectangle bounds) {
        if (!this.vScrollVisible) return null;
        float barWidth = this.barWidth.value();
        return new Rectangle(bounds.width() - barWidth, 0, barWidth, bounds.height() - (this.hScrollVisible && this.barType.value() == ScrollBarType.NORMAL ? barWidth : 0));
    }

    @Nullable
    private Rectangle getVRailBounds(final Rectangle bounds) {
        return this.getVScrollArea(bounds);
    }

    @Nullable
    private Rectangle getVThumbBounds(final Rectangle bounds) {
        Rectangle rail = this.getVRailBounds(bounds);
        if (rail == null) return null;
        float contentHeight = this.childSize.height();
        float visibleHeight = this.visibleHeight(bounds);
        float thumbHeight = Math.max(20, (visibleHeight / contentHeight) * rail.height());
        float maxScroll = Math.max(1, contentHeight - visibleHeight);
        float scrollPercentage = this.scrollY / maxScroll;
        float thumbY = rail.y() + scrollPercentage * (rail.height() - thumbHeight);
        return new Rectangle(rail.x(), thumbY, rail.width(), thumbHeight);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return this.child.computeIdealSize(new Size(
                this.horizontalScrolling ? Float.MAX_VALUE : constraints.width(),
                this.verticalScrolling ? Float.MAX_VALUE : constraints.height()
        ));
    }

    @Override
    public void computeLayout(final Size size) {
        float previousMaxScrollX = this.childSize.width() - this.visibleWidth(new Rectangle(size));
        float previousMaxScrollY = this.childSize.height() - this.visibleHeight(new Rectangle(size));
        float availableWidth = size.width();
        float availableHeight = size.height();

        this.hScrollVisible = false;
        this.vScrollVisible = false;
        if (this.barType.value() == ScrollBarType.NORMAL) {
            Size idealChildSize = this.child.computeIdealSize(new Size(
                    this.horizontalScrolling ? Float.MAX_VALUE : size.width(),
                    this.verticalScrolling ? Float.MAX_VALUE : size.height()
            ));

            this.hScrollVisible = this.horizontalScrolling && idealChildSize.width() > (this.vScrollVisible ? availableWidth - this.barWidth.value() : availableWidth);
            this.vScrollVisible = this.verticalScrolling && idealChildSize.height() > availableHeight;
            if (this.hScrollVisible && !this.vScrollVisible) {
                this.vScrollVisible = this.verticalScrolling && idealChildSize.height() > (availableHeight - this.barWidth.value());
            }

            if (this.hScrollVisible) availableHeight -= this.barWidth.value();
            if (this.vScrollVisible) availableWidth -= this.barWidth.value();
        }

        Size childSize = new Size(
                MathUtils.clamp(availableWidth, this.child.minSize().width(), this.child.maxSize().width()),
                MathUtils.clamp(availableHeight, this.child.minSize().height(), this.child.maxSize().height())
        );
        this.child.computeLayout(childSize);
        if (this.child instanceof Parent parent) {
            Size parentContentSize = parent.contentSize();
            if (!parentContentSize.equals(Size.EMPTY)) {
                childSize = new Size(
                        this.horizontalScrolling ? MathUtils.clamp(Math.max(childSize.width(), parentContentSize.width()), this.child.minSize().width(), this.child.maxSize().width()) : childSize.width(),
                        this.verticalScrolling ? MathUtils.clamp(Math.max(childSize.height(), parentContentSize.height()), this.child.minSize().height(), this.child.maxSize().height()) : childSize.height()
                );
            }
        }
        this.childSize = childSize;

        float oldScrollX = this.scrollX;
        float oldScrollY = this.scrollY;
        { // Horizontal scroll bar
            float contentWidth = childSize.width();
            if (this.barType.value() == ScrollBarType.FLOATING) {
                this.hScrollVisible = this.horizontalScrolling && contentWidth > availableWidth;
            }
            float maxScrollX = Math.max(0, contentWidth - availableWidth);
            this.targetScrollX = MathUtils.clamp(this.targetScrollX, 0, maxScrollX);
            this.scrollX = Snapping.snap(this.rivet(), MathUtils.clamp(this.scrollX, 0, maxScrollX));
        }
        { // Vertical scroll bar
            float contentHeight = childSize.height();
            if (this.barType.value() == ScrollBarType.FLOATING) {
                this.vScrollVisible = this.verticalScrolling && contentHeight > availableHeight;
            }
            float maxScrollY = Math.max(0, contentHeight - availableHeight);
            if (this.autoScroll && previousMaxScrollY - this.targetScrollY <= availableHeight * this.autoScrollThreshold) {
                this.targetScrollY = maxScrollY;
            } else {
                this.targetScrollY = MathUtils.clamp(this.targetScrollY, 0, maxScrollY);
            }
            this.scrollY = Snapping.snap(this.rivet(), MathUtils.clamp(this.scrollY, 0, maxScrollY));
        }
        if (oldScrollX != this.scrollX || oldScrollY != this.scrollY) {
            this.onScroll();
        }
    }

    @Override
    public void requestLayoutRecalculation() {
        if (this.parent() != null) this.parent().requestLayoutRecalculation();
    }

    @Override
    public Size contentSize() {
        return Size.EMPTY;
    }

    private void onScroll() {
        this.scrollListener.callVoid(c -> c.onScroll(this.scrollX, this.scrollY));
    }


    public enum ScrollBarType {
        FLOATING, NORMAL
    }

    private class NestedScrollCoordinator {
        @Nullable
        private ScrollTarget lastTarget;
        private long lastScroll;

        public boolean handleScrolling(final BooleanSupplier parent, final BooleanSupplier child) {
            if (System.nanoTime() - this.lastScroll > TimeUnit.MILLISECONDS.toNanos(ScrollContainer.this.nestedScrollTimeout.value())) {
                this.lastTarget = null;
            }
            if (this.lastTarget == null || this.lastTarget.equals(ScrollTarget.CHILD)) {
                if (child.getAsBoolean()) {
                    this.lastTarget = ScrollTarget.CHILD;
                    this.lastScroll = System.nanoTime();
                    return true;
                }
            }
            if (this.lastTarget == null || this.lastTarget.equals(ScrollTarget.PARENT)) {
                if (parent.getAsBoolean()) {
                    this.lastTarget = ScrollTarget.PARENT;
                    this.lastScroll = System.nanoTime();
                    return true;
                }
            }
            return false;
        }


        private enum ScrollTarget {
            PARENT, CHILD
        }
    }


    @FunctionalInterface
    public interface ScrollListener {
        void onScroll(final float scrollX, final float scrollY);
    }

}
