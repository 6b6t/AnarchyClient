package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.TextValueSetting;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.impl.TextField;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;

import static net.blockhost.anarchyclient.ui.PanelStyle.DESCRIPTION_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.DIVIDER;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT_FIELD_ROW_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT_ROW_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.label;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.settingDescription;
import static net.blockhost.anarchyclient.ui.PanelWidgets.surface;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textField;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

final class TextSettingRow extends Container implements LayoutDebugLabel {

    private final Setting<?> setting;
    private final TextValueSetting textSetting;
    private final Surface divider = surface(DIVIDER);
    private final TextNode label;
    // Null when the setting has no description; the row then collapses that line.
    private final TextNode description;
    private final TextField field;
    private final Runnable save;

    TextSettingRow(final Setting<?> setting, final TextValueSetting textSetting, final Runnable save) {
        super(gridLayout(0, 0));
        this.setting = setting;
        this.textSetting = textSetting;
        this.save = save;
        this.label = textNode(setting.name(), MUTED);
        this.description = settingDescription(setting);
        this.field = textField(textSetting.valueString());
        this.field.valueChangeListener().add(value -> {
            try {
                this.textSetting.valueFromString(value);
                this.save.run();
            } catch (IllegalArgumentException ignored) {
                // Keep the last valid value while an incomplete structured value is being typed.
            }
        });
        this.addChild(this.divider);
        this.addChild(this.label);
        if (this.description != null) {
            this.addChild(this.description);
        }
        this.addChild(this.field);
    }

    @Override
    public void computeLayout(final Size size) {
        float extra = this.extraHeight(size.width());
        place(this.divider, cell(0, 2, 2, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                padding(PADDING, 0F, PADDING, 0F), null, 1F));
        place(this.label, cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING, 4F, 8F, 0F), null, 18F));
        if (this.description != null) {
            place(this.description, cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, 8F, 5F), null, DESCRIPTION_HEIGHT + extra));
        }
        place(this.field, cell(1, 0, 1, 2, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(0F, 7F, 10F, 7F), null, 24F));
        super.computeLayout(size);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        // Without a description the row still has to fit the text field.
        float height = this.description == null ? TEXT_FIELD_ROW_HEIGHT : TEXT_ROW_HEIGHT;
        return new Size(constraints.width(), height + this.extraHeight(constraints.width()));
    }

    private float extraHeight(final float rowWidth) {
        if (this.description == null) {
            return 0F;
        }
        // The text field takes roughly the right half of the row.
        return (this.description.lineCount(Math.max(0F, rowWidth * 0.5F - PADDING - 8F)) - 1) * 11F;
    }

    @Override
    public String layoutDebugLabel() {
        return this.setting.id();
    }
}