package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.profile.ProfileManager;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;

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

final class ProfileRow extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final ProfileManager.ProfileSummary profile;
    private final Surface background = new Surface(() -> CARD).cornerRadius(10F);
    private final TextNode name;
    private final TextNode modules;
    private final TextAction apply;
    private final TextAction delete;

    ProfileRow(final ModulePanel panel, final ProfileManager.ProfileSummary profile) {
        super(gridLayout(0, 0));
        this.panel = panel;
        this.profile = profile;
        this.fixedSize(-1, 44);
        this.name = textNode(profile.name(), TEXT);
        this.modules = textNode(Translations.format("anarchyclient.ui.profile_module_count", "%s modules", profile.modules()), FAINT);
        this.apply = textAction(Translations.text("anarchyclient.ui.apply", "Apply"), GlassTheme::accent, () -> {
            AnarchyClient.PROFILES.apply(this.profile.name(), this.panel.modules);
            this.panel.config.save();
            this.panel.refreshModuleList();
            this.panel.inspector.refresh();
        });
        this.delete = textAction(Translations.text("anarchyclient.ui.delete", "Delete"), MUTED, () -> {
            if (AnarchyClient.PROFILES.delete(this.profile.name())) {
                this.panel.profilesPanel.refresh();
            }
        });
        place(this.background, fillCell(0, 0, 3, 2));
        place(this.name, cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING, 0F, 8F, 0F), null, 18F));
        place(this.modules, cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(PADDING, 0F, 8F, 0F), null, 18F));
        // Size each action to its text, right-aligned in its column: a click on the empty space next to
        // Translations.text("anarchyclient.ui.apply", "Apply") used to land on Translations.text("anarchyclient.ui.delete", "Delete").
        place(this.apply, cell(1, 0, 1, 2, 0F, 1F, GridAnchor.RIGHT, GridFill.NONE,
                padding(0F, 0F, 10F, 0F), 52F, null));
        place(this.delete, cell(2, 0, 1, 2, 0F, 1F, GridAnchor.RIGHT, GridFill.NONE,
                padding(0F, 0F, PADDING, 0F), 50F, null));
        this.addChild(this.background);
        this.addChild(this.name);
        this.addChild(this.modules);
        this.addChild(this.apply);
        this.addChild(this.delete);
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