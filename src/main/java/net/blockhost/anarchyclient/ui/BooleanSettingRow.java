package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;

import static net.blockhost.anarchyclient.ui.PanelStyle.SETTING_ROW_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_WIDTH;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;

final class BooleanSettingRow extends SettingLine {

    private final BooleanSetting setting;
    private final Runnable onChange;
    private final ToggleSwitch toggle;

    BooleanSettingRow(final BooleanSetting setting, final Runnable onChange) {
        super(setting, SETTING_ROW_HEIGHT);
        this.setting = setting;
        this.onChange = onChange;
        this.toggle = new ToggleSwitch(this.setting::value, this::toggle);
        this.addChild(this.toggle);
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
        if (super.onComponentMouseDown(event, size)) {
            return true;
        }
        if (event.button() == MouseButton.LEFT) {
            this.toggle();
            return true;
        }
        return false;
    }

    @Override
    protected void layoutControls(final Size size) {
        place(this.toggle, cell(3, 0, 1, 2, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                padding(0F, 0F, 14F, 0F), SWITCH_WIDTH, SWITCH_HEIGHT));
    }

    private void toggle() {
        this.setting.value(!this.setting.value());
        this.onChange.run();
    }
}