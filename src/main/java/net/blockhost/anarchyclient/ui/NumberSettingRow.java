package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.setting.NumberSetting;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.impl.slider.Slider;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

import static net.blockhost.anarchyclient.ui.PanelStyle.DESCRIPTION_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.DIVIDER;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.NUMBER_ROW_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.label;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.settingDescription;
import static net.blockhost.anarchyclient.ui.PanelWidgets.slider;
import static net.blockhost.anarchyclient.ui.PanelWidgets.surface;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

final class NumberSettingRow extends Container implements LayoutDebugLabel {

    private final NumberSetting setting;
    private final Surface divider = surface(DIVIDER);
    private final TextNode label;
    // Null when the setting has no description; the row then collapses that line.
    private final TextNode description;
    private final TextNode value;
    private final Slider slider;
    private final Runnable save;

    NumberSettingRow(final NumberSetting setting, final Runnable save) {
        super(gridLayout(0, 0));
        this.setting = setting;
        this.save = save;
        this.label = textNode(setting.name(), MUTED);
        this.description = settingDescription(setting);
        this.value = textNode(() -> SettingControls.displayValue(this.setting), () -> TEXT)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        // Step 0: Rivet passes the raw position through, and the setting snaps it once, from its minimum. Letting
        // Rivet snap too (from 0) put the value up to a step away from the pointer.
        this.slider = slider(setting.min(), setting.max(), 0.0, setting.value());
        this.slider.valueChangeListener().add(value -> {
            double snapped = setting.snap(value);
            // Keep the thumb on the grid; value() does not fire this listener again.
            this.slider.value(snapped);
            if (snapped != setting.value()) {
                setting.value(snapped);
                this.save.run();
            }
        });
        this.addChild(this.divider);
        this.addChild(this.label);
        if (this.description != null) {
            this.addChild(this.description);
        }
        this.addChild(this.value);
        this.addChild(this.slider);
    }

    @Override
    public void computeLayout(final Size size) {
        float valueWidth = Math.min(58F, Math.max(0F, size.width() * 0.25F));
        float extra = this.extraHeight(size.width());
        place(this.divider, cell(0, 3, 2, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                padding(PADDING, 0F, PADDING, 0F), null, 1F));
        place(this.label, cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING, 4F, 8F, 0F), null, 18F));
        place(this.value, cell(1, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                padding(0F, 4F, PADDING, 0F), valueWidth, 18F));
        if (this.description != null) {
            place(this.description, cell(0, 1, 2, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, PADDING, 0F), null, DESCRIPTION_HEIGHT + extra));
        }
        place(this.slider, cell(0, 2, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.HORIZONTAL,
                padding(PADDING, 0F, PADDING, 0F), null, 16F));
        super.computeLayout(size);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        float height = this.description == null ? NUMBER_ROW_HEIGHT - DESCRIPTION_HEIGHT : NUMBER_ROW_HEIGHT;
        return new Size(constraints.width(), height + this.extraHeight(constraints.width()));
    }

    private float extraHeight(final float rowWidth) {
        if (this.description == null) {
            return 0F;
        }
        return (this.description.lineCount(Math.max(0F, rowWidth - PADDING * 2F)) - 1) * 11F;
    }

    @Override
    public String layoutDebugLabel() {
        return this.setting.id();
    }
}