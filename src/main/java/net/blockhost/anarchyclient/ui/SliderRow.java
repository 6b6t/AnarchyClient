package net.blockhost.anarchyclient.ui;

import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.impl.slider.Slider;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.NUMBER_ROW_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.label;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.slider;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

/**
 * Label + live value + slider driving a global theme token.
 */
final class SliderRow extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final String name;
    private final TextNode label;
    private final TextNode description;
    private final TextNode value;
    private final Slider slider;

    SliderRow(final ModulePanel panel, final String name, final String description, final float min, final float max, final float step,
              final Supplier<Float> get, final Consumer<Float> set, final Supplier<String> display) {
        super(gridLayout(0, 0));
        this.panel = panel;
        this.name = name;
        this.label = textNode(name, MUTED);
        this.description = textNode(description, () -> FAINT);
        this.value = textNode(display, () -> TEXT)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        this.slider = slider(min, max, step, get.get());
        this.slider.valueChangeListener().add(newValue -> {
            set.accept(newValue.floatValue());
            this.panel.saveUiPreferencesLater();
        });
        this.fixedSize(-1, NUMBER_ROW_HEIGHT);
        this.addChild(this.label);
        this.addChild(this.description);
        this.addChild(this.value);
        this.addChild(this.slider);
    }

    @Override
    public void computeLayout(final Size size) {
        float valueWidth = Math.min(58F, Math.max(0F, size.width() * 0.25F));
        place(this.label, cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING, 4F, 8F, 0F), null, 18F));
        place(this.value, cell(1, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                padding(0F, 4F, PADDING, 0F), valueWidth, 18F));
        place(this.description, cell(0, 1, 2, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING, 0F, PADDING, 0F), null, 17F));
        place(this.slider, cell(0, 2, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.HORIZONTAL,
                padding(PADDING, 0F, PADDING, 0F), null, 16F));
        super.computeLayout(size);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), NUMBER_ROW_HEIGHT);
    }

    @Override
    public String layoutDebugLabel() {
        return this.name;
    }
}