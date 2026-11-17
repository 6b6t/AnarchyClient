package net.blockhost.anarchyclient.ui;

import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;

import java.util.Locale;

import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.label;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

final class GroupHeader extends Container implements LayoutDebugLabel {

    private final String name;
    private final TextNode label;

    GroupHeader(final String name) {
        super(gridLayout(0, 0));
        this.name = name;
        this.fixedSize(-1, 24);
        this.label = textNode(name.toUpperCase(Locale.ROOT), FAINT);
        place(this.label, cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING, 4F, PADDING, 0F), null, null));
        this.addChild(this.label);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), 24);
    }

    @Override
    public String layoutDebugLabel() {
        return this.name;
    }
}