package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.profile.ProfileManager;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.component.impl.TextField;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.layout.list.VerticalListLayout;
import net.lenni0451.rivet.math.Size;

import java.util.List;

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

final class ProfilesPanel extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final GlassSurface background;
    private final TextNode title;
    private final TextField nameField;
    private final Button saveButton;
    private final Container rows;
    private final ScrollContainer scroll;

    ProfilesPanel(final ModulePanel panel) {
        super(gridLayout(8, 4));
        this.panel = panel;
        this.background = this.panel.glassPanel();
        this.title = textNode("Profiles", TEXT);
        this.nameField = textField("");
        this.saveButton = button("Capture", TEXT, ignored -> this.capture());
        this.rows = new Container(new VerticalListLayout(4, true));
        this.scroll = new LayoutDebugScrollContainer(this.rows);
        this.panel.configureScroll(this.scroll);
        onEnter(this.nameField, this::capture);
        place(this.background, fillCell(0, 0, 2, 3));
        place(this.title, cell(0, 0, 2, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING + 2F, 8F, PADDING, 0F), null, 26F));
        place(this.nameField, cell(0, 1, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING, 0F, 0F, 0F), null, null));
        place(this.saveButton, cell(1, 1, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                padding(0F, 0F, PADDING, 0F), 84F, 28F));
        place(this.scroll, cell(0, 2, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING, 4F, PADDING, PADDING), null, null));
        this.addChild(this.background);
        this.addChild(this.title);
        this.addChild(this.nameField);
        this.addChild(this.saveButton);
        this.addChild(this.scroll);
    }

    void refresh() {
        this.rows.clearChildren();
        List<ProfileManager.ProfileSummary> profiles = AnarchyClient.PROFILES.summaries();
        if (profiles.isEmpty()) {
            this.rows.addChild(new EmptyState("No profiles captured."));
        } else {
            for (ProfileManager.ProfileSummary profile : profiles) {
                this.rows.addChild(new ProfileRow(this.panel, profile));
            }
        }
        this.panel.requestFrame();
    }

    private void capture() {
        String name = this.nameField.text().trim();
        if (name.isEmpty()) {
            return;
        }
        AnarchyClient.PROFILES.capture(name, this.panel.modules);
        this.nameField.text("");
        this.refresh();
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return constraints;
    }

    @Override
    public String layoutDebugLabel() {
        return "profiles=" + AnarchyClient.PROFILES.summaries().size();
    }
}