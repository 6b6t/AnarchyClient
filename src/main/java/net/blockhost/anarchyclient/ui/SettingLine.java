package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.setting.Setting;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;

import static net.blockhost.anarchyclient.ui.PanelStyle.DIVIDER;
import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_WIDTH;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.label;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.surface;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

abstract class SettingLine extends Container implements LayoutDebugLabel {

    private static final float WRAP_LINE_HEIGHT = 11F;

    private final Setting<?> setting;
    private final float height;
    private final Surface divider = surface(DIVIDER);
    private final TextNode label;
    private final TextNode description;

    SettingLine(final Module module, final Setting<?> setting, final float height) {
        super(gridLayout(0, 0));
        this.setting = setting;
        this.height = height;
        this.label = textNode(setting.name(), MUTED);
        this.description = textNode(() -> module.settingDescription(setting), () -> FAINT).maxLines(Integer.MAX_VALUE);
        this.addChild(this.divider);
        this.addChild(this.label);
        this.addChild(this.description);
    }

    @Override
    public void computeLayout(final Size size) {
        float extra = this.extraHeight(size.width());
        place(this.divider, cell(0, 2, 4, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                padding(PADDING, 0F, PADDING, 0F), null, 1F));
        place(this.label, cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING, 4F, 8F, 0F), null, 18F));
        place(this.description, cell(0, 1, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING, 0F, 8F, 5F), null, 17F + extra));
        this.layoutControls(size);
        super.computeLayout(size);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), this.height + this.extraHeight(constraints.width()));
    }

    /** Row grows when the description folds onto a second line instead of ellipsizing. */
    private float extraHeight(final float rowWidth) {
        float available = Math.max(0F, rowWidth - PADDING - 8F - this.controlReserve(rowWidth));
        return (this.description.lineCount(available) - 1) * WRAP_LINE_HEIGHT;
    }

    /** Horizontal space claimed by this row's control columns, kept clear of the description. */
    protected float controlReserve(final float rowWidth) {
        return SWITCH_WIDTH + 14F;
    }

    protected void layoutControls(final Size size) {
    }

    @Override
    public String layoutDebugLabel() {
        return this.setting.id();
    }
}