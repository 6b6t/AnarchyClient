package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.SETTING_ROW_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

import net.blockhost.anarchyclient.setting.Setting;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

final class ValueSettingRow extends SettingLine {

    private final Setting<?> setting;
    private final TextNode value;

    ValueSettingRow(final Setting<?> setting) {
        super(setting, SETTING_ROW_HEIGHT);
        this.setting = setting;
        this.value = textNode(() -> SettingControls.displayValue(this.setting), () -> TEXT)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        this.addChild(this.value);
    }

    @Override
    protected float controlReserve(final float rowWidth) {
        return Math.min(112F, Math.max(0F, rowWidth * 0.44F)) + PADDING;
    }

    @Override
    protected void layoutControls(final Size size) {
        float valueWidth = Math.min(112F, Math.max(0F, size.width() * 0.44F));
        place(
                this.value,
                cell(
                        2,
                        0,
                        2,
                        2,
                        0F,
                        1F,
                        GridAnchor.CENTER,
                        GridFill.BOTH,
                        padding(0F, 0F, PADDING, 0F),
                        valueWidth,
                        null));
    }
}
