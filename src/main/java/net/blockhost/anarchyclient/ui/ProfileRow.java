package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.CARD;
import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
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
import net.blockhost.anarchyclient.profile.ProfileManager;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;

final class ProfileRow extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final ProfileManager.ProfileSummary profile;

    ProfileRow(final ModulePanel panel, final ProfileManager.ProfileSummary profile) {
        super(gridLayout(0, 0));
        this.panel = panel;
        this.profile = profile;
        this.fixedSize(-1, 44);
        Surface background = new Surface(() -> CARD).cornerRadius(10F);
        TextNode name = textNode(profile.name(), TEXT);
        TextNode modules = textNode(
                Translations.format("anarchyclient.ui.profile_module_count", "%s modules", profile.modules()), FAINT);
        TextAction apply = textAction(Translations.text("anarchyclient.ui.apply", "Apply"), GlassTheme::accent, () -> {
            AnarchyClient.PROFILES.apply(this.profile.name(), this.panel.modules);
            this.panel.config.save();
            this.panel.refreshModuleList();
            this.panel.inspector.refresh();
        });
        TextAction delete = textAction(Translations.text("anarchyclient.ui.delete", "Delete"), MUTED, () -> {
            if (AnarchyClient.PROFILES.delete(this.profile.name())) {
                this.panel.profilesPanel.refresh();
            }
        });
        place(background, fillCell(0, 0, 3, 2));
        place(
                name,
                cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH, padding(PADDING, 0F, 8F, 0F), null, 18F));
        place(
                modules,
                cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH, padding(PADDING, 0F, 8F, 0F), null, 18F));
        // Size each action to its text (no fixed column width): a click on the empty space next to "Apply" used to
        // land on "Delete", and fixed widths cut off longer translations.
        place(apply, cell(1, 0, 1, 2, 0F, 1F, GridAnchor.RIGHT, GridFill.NONE, padding(0F, 0F, 10F, 0F), null, null));
        place(
                delete,
                cell(2, 0, 1, 2, 0F, 1F, GridAnchor.RIGHT, GridFill.NONE, padding(0F, 0F, PADDING, 0F), null, null));
        this.addChild(background);
        this.addChild(name);
        this.addChild(modules);
        this.addChild(apply);
        this.addChild(delete);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(constraints.width(), 44);
    }

    @Override
    public String layoutDebugLabel() {
        return this.profile.name();
    }
}
