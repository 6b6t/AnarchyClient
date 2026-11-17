package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.setting.SelectSetting;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BOX;
import static net.blockhost.anarchyclient.ui.PanelStyle.SETTING_ROW_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.iconNode;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

final class SelectSettingRow extends SettingLine {

    private final SelectSetting setting;
    private final Runnable onChange;
    private final TextNode value;
    private final IconNode icon = iconNode("chevron-down", FAINT);

    SelectSettingRow(final SelectSetting setting, final Runnable onChange) {
        super(setting, SETTING_ROW_HEIGHT);
        this.setting = setting;
        this.onChange = onChange;
        this.value = textNode(this.setting::value, () -> TEXT)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        this.addChild(this.value);
        this.addChild(this.icon);
    }

    @Override
    protected float controlReserve(final float rowWidth) {
        return Math.min(100F, Math.max(0F, rowWidth * 0.42F)) + ICON_BOX + 10F;
    }

    @Override
    protected void layoutControls(final Size size) {
        float valueWidth = Math.min(100F, Math.max(0F, size.width() * 0.42F));
        place(this.value, cell(2, 0, 1, 2, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(0F, 0F, 2F, 0F), valueWidth, null));
        place(this.icon, cell(3, 0, 1, 2, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                padding(0F, 0F, 8F, 0F), ICON_BOX, ICON_BOX));
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
        if (event.button() == MouseButton.LEFT) {
            this.setting.next();
            this.onChange.run();
            return true;
        }
        return false;
    }
}