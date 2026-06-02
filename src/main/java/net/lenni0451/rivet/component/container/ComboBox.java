package net.lenni0451.rivet.component.container;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.ListenerList;
import net.lenni0451.rivet.component.Parent;
import net.lenni0451.rivet.component.impl.Label;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.layer.Layer;
import net.lenni0451.rivet.layer.LayerBucket;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayout;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayoutOptions;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.theme.Theme;
import net.lenni0451.rivet.theme.ThemeOption;

import java.util.function.Consumer;

@Accessors(fluent = true, chain = true, makeFinal = true)
public class ComboBox extends Component implements Parent {

    @Getter
    private final Button button;
    @Getter
    private final Component child;
    @Getter
    private final ListenerList<Runnable> openListener = new ListenerList<>();
    @Getter
    private final ListenerList<Runnable> closeListener = new ListenerList<>();
    private Layer layer;

    @Getter
    private final ThemeOption<Color> arrowColor;
    @Getter
    private final ThemeOption<Float> arrowSize;
    @Getter
    private final ThemeOption<Float> maxPopupHeight;

    public ComboBox(final String text, final Component child) {
        this(text, child, c -> {});
    }

    public <C extends Component> ComboBox(final String text, final C child, final Consumer<C> initializer) {
        this(new Label(text).horizontalOrigin(TextOrigin.Horizontal.VISUAL_LEFT), t -> {}, child, initializer);
    }

    public <T extends Component, C extends Component> ComboBox(final T text, final C child) {
        this(text, t -> {}, child, c -> {});
    }

    public <T extends Component, C extends Component> ComboBox(final T text, final Consumer<T> textInitializer, final C child, final Consumer<C> initializer) {
        this.button = new Button(text, event -> {
            if (event.button().equals(MouseButton.LEFT)) {
                if (this.isOpen()) {
                    this.close();
                } else {
                    this.open();
                }
            }
        });
        this.child = child;
        textInitializer.accept(text);
        initializer.accept(child);

        this.arrowColor = new ThemeOption<>(this, Theme.COMBOBOX_ARROW_COLOR);
        this.arrowSize = new ThemeOption<>(this, Theme.COMBOBOX_ARROW_SIZE);
        this.maxPopupHeight = new ThemeOption<>(this, Theme.COMBOBOX_MAX_POPUP_HEIGHT);
    }

    public ComboBox open() {
        if (this.isOpen()) return this;
        Container container = new Container(AbsoluteLayout.INSTANCE);
        container.addChild(this.child);
        this.layer = new Layer(container, LayerBucket.OVERLAY);
        this.rivet().addLayer(this.layer);
        this.openListener.callVoid(Runnable::run);
        return this;
    }

    public ComboBox close() {
        if (!this.isOpen()) return this;
        this.rivet().removeLayer(this.layer);
        this.layer = null;
        this.closeListener.callVoid(Runnable::run);
        return this;
    }

    public boolean isOpen() {
        return this.layer != null;
    }

    @Override
    protected void onComponentAdded() {
        this.button.setRivet(this.rivet(), this);
    }

    @Override
    protected void onComponentRemoved() {
        this.close();
        this.button.setRivet(null, null);
    }

    @Override
    protected void onComponentMouseEnter() {
        this.button.onMouseEnter();
    }

    @Override
    protected void onComponentMouseLeave() {
        this.button.onMouseLeave();
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        return this.button.onMouseDown(event, bounds);
    }

    @Override
    protected boolean onComponentMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
        return this.button.onMouseUp(event, bounds);
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        this.button.render(renderer, bounds);
        float triangleSize = this.arrowSize.value();
        if (this.isOpen()) {
            renderer.fillTriangle(
                    bounds.width() - this.button.innerPadding().value().right() - triangleSize,
                    bounds.height() / 2F + triangleSize / 2F,
                    bounds.width() - this.button.innerPadding().value().right(),
                    bounds.height() / 2F + triangleSize / 2F,
                    bounds.width() - this.button.innerPadding().value().right() - triangleSize / 2F,
                    bounds.height() / 2F - triangleSize / 2F,
                    this.arrowColor.value()
            );
        } else {
            renderer.fillTriangle(
                    bounds.width() - this.button.innerPadding().value().right() - triangleSize,
                    bounds.height() / 2F - triangleSize / 2F,
                    bounds.width() - this.button.innerPadding().value().right() - triangleSize / 2F,
                    bounds.height() / 2F + triangleSize / 2F,
                    bounds.width() - this.button.innerPadding().value().right(),
                    bounds.height() / 2F - triangleSize / 2F,
                    this.arrowColor.value()
            );
        }
        if (this.isOpen()) {
            Size screenSize = this.rivet().scaledSize();
            Rectangle region = new Rectangle(
                    bounds.x(),
                    bounds.y() + bounds.height(),
                    Math.min(screenSize.width() - bounds.x(), bounds.width()),
                    screenSize.height() - bounds.y() - bounds.height()
            );
            Size idealSize = this.child.computeIdealSize(region.size());
            float maxHeight = Math.min(region.height(), this.maxPopupHeight.value());
            region = region.withHeight(Math.min(idealSize.height(), maxHeight));
            if (!(this.child.layoutOptions() instanceof AbsoluteLayoutOptions options)
                    || options.x() != region.x() || options.y() != region.y()
                    || options.width() == null || options.width() != region.width()
                    || options.height() == null || options.height() != region.height()) {
                this.child.layoutOptions(new AbsoluteLayoutOptions(region));
            }
        }
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return this.button.computeIdealSize(constraints);
    }

    @Override
    public void requestLayoutRecalculation() {
        if (this.parent() != null) this.parent().requestLayoutRecalculation();
    }

    @Override
    public Size contentSize() {
        return this.button.contentSize();
    }

}
