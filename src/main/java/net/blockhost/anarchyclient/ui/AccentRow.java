package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;

/**
 * Accent preset swatches: one circle per preset, ringed when selected.
 */
final class AccentRow extends Container implements LayoutDebugLabel {

    static final float SWATCH_BOX = 26F;

    AccentRow(final ModulePanel panel) {
        super(gridLayout(4, 0));
        ClientConfig.GuiThemePreset[] presets = ClientConfig.GuiThemePreset.values();
        for (int index = 0; index < presets.length; index++) {
            AccentSwatch swatch = new AccentSwatch(panel, presets[index]);
            place(
                    swatch,
                    cell(
                            index,
                            0,
                            1,
                            1,
                            0F,
                            0F,
                            GridAnchor.LEFT,
                            GridFill.NONE,
                            padding(index == 0 ? PADDING : 0F, 0F, 0F, 0F),
                            SWATCH_BOX,
                            SWATCH_BOX));
            this.addChild(swatch);
        }
        this.fixedSize(-1, 36F);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), 36F);
    }

    @Override
    public String layoutDebugLabel() {
        return "accent=" + GlassTheme.preset().key();
    }
}
