package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.CARD;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.fillCell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textAction;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.i18n.Translations;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;

final class FriendRow extends Container implements LayoutDebugLabel {

    private static final int HEAD_SIZE = 20;

    private final ModulePanel panel;

    private final String friend;

    FriendRow(final ModulePanel panel, final String friend) {
        super(gridLayout(0, 0));
        this.panel = panel;
        this.friend = friend;
        this.fixedSize(-1, 34);
        Surface background = new Surface(() -> CARD).cornerRadius(10F);
        FriendHead head = new FriendHead(friend, HEAD_SIZE);
        TextNode name = textNode(friend, TEXT);
        TextAction remove = textAction(Translations.text("anarchyclient.ui.remove", "Remove"), MUTED, () -> {
            if (AnarchyClient.FRIENDS.remove(this.friend)) {
                this.panel.friendsPanel.refresh();
            }
        });
        place(background, fillCell(0, 0, 3, 1));
        place(
                head,
                cell(
                        0,
                        0,
                        1,
                        1,
                        0F,
                        1F,
                        GridAnchor.CENTER,
                        GridFill.NONE,
                        padding(PADDING, 0F, 0F, 0F),
                        (float) HEAD_SIZE,
                        (float) HEAD_SIZE));
        place(name, cell(1, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH, padding(10F, 0F, 8F, 0F), null, null));
        // Size the action to its text (no fixed column width) so only the visible "Remove" reacts to clicks and
        // longer translations are not cut off.
        place(
                remove,
                cell(2, 0, 1, 1, 0F, 1F, GridAnchor.RIGHT, GridFill.NONE, padding(0F, 0F, PADDING, 0F), null, null));
        this.addChild(background);
        this.addChild(head);
        this.addChild(name);
        this.addChild(remove);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), 34);
    }

    @Override
    public String layoutDebugLabel() {
        return this.friend;
    }
}
