package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

final class EmptyState extends Container implements LayoutDebugLabel {

    private final String message;
    private final TextNode label;

    EmptyState(final String message) {
        super(gridLayout(0, 0));
        this.message = message;
        this.fixedSize(-1, 64);
        this.label = textNode(message, FAINT);
        this.label.origin(TextOrigin.Horizontal.VISUAL_CENTER, TextOrigin.Vertical.LOGICAL_CENTER);
        place(
                this.label,
                cell(
                        0,
                        0,
                        1,
                        1,
                        1F,
                        1F,
                        GridAnchor.CENTER,
                        GridFill.BOTH,
                        padding(PADDING, 0F, PADDING, 0F),
                        null,
                        null));
        this.addChild(this.label);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), 64);
    }

    @Override
    public String layoutDebugLabel() {
        return this.message;
    }
}
