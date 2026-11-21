package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.button;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.fillCell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.onEnter;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textField;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

import java.util.Collection;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.i18n.Translations;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.component.impl.TextField;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.layout.list.VerticalListLayout;
import net.lenni0451.rivet.math.Size;

final class FriendsPanel extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final GlassSurface background;
    private final TextNode title;
    private final TextField addField;
    private final Button addButton;
    private final Container rows;
    private final ScrollContainer scroll;

    FriendsPanel(final ModulePanel panel) {
        super(gridLayout(8, 4));
        this.panel = panel;
        this.background = this.panel.glassPanel();
        this.title = textNode(Translations.text("anarchyclient.ui.friends", "Friends"), TEXT);
        this.addField = textField("");
        this.addButton = button(Translations.text("anarchyclient.ui.add", "Add"), TEXT, ignored -> this.addFriend());
        this.rows = new Container(new VerticalListLayout(4, true));
        this.scroll = new LayoutDebugScrollContainer(this.rows);
        this.panel.configureScroll(this.scroll);
        onEnter(this.addField, this::addFriend);
        place(this.background, fillCell(0, 0, 2, 3));
        place(
                this.title,
                cell(
                        0,
                        0,
                        2,
                        1,
                        1F,
                        0F,
                        GridAnchor.CENTER,
                        GridFill.BOTH,
                        padding(PADDING + 2F, 8F, PADDING, 0F),
                        null,
                        26F));
        place(
                this.addField,
                cell(0, 1, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH, padding(PADDING, 0F, 0F, 0F), null, null));
        place(
                this.addButton,
                cell(1, 1, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH, padding(0F, 0F, PADDING, 0F), 62F, 28F));
        place(
                this.scroll,
                cell(
                        0,
                        2,
                        2,
                        1,
                        1F,
                        1F,
                        GridAnchor.CENTER,
                        GridFill.BOTH,
                        padding(PADDING, 4F, PADDING, PADDING),
                        null,
                        null));
        this.addChild(this.background);
        this.addChild(this.title);
        this.addChild(this.addField);
        this.addChild(this.addButton);
        this.addChild(this.scroll);
    }

    void refresh() {
        this.rows.clearChildren();
        Collection<String> friends = AnarchyClient.FRIENDS.friends();
        if (friends.isEmpty()) {
            this.rows.addChild(
                    new EmptyState(Translations.text("anarchyclient.ui.no_friends_saved", "No friends saved.")));
        } else {
            for (String friend : friends) {
                this.rows.addChild(new FriendRow(this.panel, friend));
            }
        }
        this.panel.requestFrame();
    }

    private void addFriend() {
        if (AnarchyClient.FRIENDS.add(this.addField.text())) {
            this.addField.text("");
            this.refresh();
        }
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return constraints;
    }

    @Override
    public String layoutDebugLabel() {
        return "friends=" + AnarchyClient.FRIENDS.friends().size();
    }
}
