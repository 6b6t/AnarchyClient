package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.DOCK_BUTTON;
import static net.blockhost.anarchyclient.ui.PanelStyle.DOCK_DIVIDER;
import static net.blockhost.anarchyclient.ui.PanelStyle.DOCK_GAP;
import static net.blockhost.anarchyclient.ui.PanelStyle.DOCK_WIDTH;
import static net.blockhost.anarchyclient.ui.PanelWidgets.categoryIcon;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.fillCell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;

import java.util.ArrayList;
import java.util.List;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.rivet.BackgroundDesign;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;

final class Dock extends Container {

    private final ModulePanel panel;

    private final List<Component> entries = new ArrayList<>();

    Dock(final ModulePanel panel) {
        super(gridLayout(0, (int) DOCK_GAP));
        GlassSurface background =
                new GlassSurface(GlassTheme::glass, () -> DOCK_WIDTH / 2F, () -> BackgroundDesign.NONE);
        this.panel = panel;
        for (ModuleCategory category : ModuleCategory.values()) {
            this.entries.add(new DockButton(
                    categoryIcon(category),
                    category.displayName(),
                    () -> this.panel.selectedTab == Tab.MODULES && this.panel.selectedCategory == category,
                    () -> this.panel.selectCategory(category),
                    () -> this.panel.enabledCount(category) > 0));
        }
        this.entries.add(new DockDivider());
        this.entries.add(new DockButton(
                "palette",
                Translations.text("anarchyclient.ui.theme", "Theme"),
                () -> this.panel.selectedTab == Tab.THEME,
                () -> this.panel.selectTab(Tab.THEME),
                () -> false));
        this.entries.add(new DockButton(
                "users",
                Translations.text("anarchyclient.ui.friends", "Friends"),
                () -> this.panel.selectedTab == Tab.FRIENDS,
                () -> this.panel.selectTab(Tab.FRIENDS),
                () -> false));
        this.entries.add(new DockButton(
                "layers",
                Translations.text("anarchyclient.ui.profiles", "Profiles"),
                () -> this.panel.selectedTab == Tab.PROFILES,
                () -> this.panel.selectTab(Tab.PROFILES),
                () -> false));
        this.entries.add(new DockDivider());
        this.entries.add(new DockButton(
                "settings",
                Translations.text("anarchyclient.ui.settings", "Settings"),
                () -> this.panel.drawer != Drawer.NONE,
                // openDrawer toggles: clicking the gear again closes the drawer.
                () -> this.panel.openDrawer(Drawer.ROOT),
                () -> false));

        place(background, fillCell(0, 0, 1, this.entries.size()));
        this.addChild(background);
        int row = 0;
        for (Component entry : this.entries) {
            float height = entry instanceof DockDivider ? DOCK_DIVIDER : DOCK_BUTTON;
            float topPad = row == 0 ? 8F : 0F;
            float bottomPad = row == this.entries.size() - 1 ? 8F : 0F;
            place(
                    entry,
                    cell(
                            0,
                            row,
                            1,
                            1,
                            1F,
                            0F,
                            GridAnchor.CENTER,
                            GridFill.BOTH,
                            padding(6F, topPad, 6F, bottomPad),
                            null,
                            height));
            this.addChild(entry);
            row++;
        }
    }

    float dockHeight() {
        float height = 16F; // top and bottom breathing room
        for (Component entry : this.entries) {
            height += entry instanceof DockDivider ? DOCK_DIVIDER : DOCK_BUTTON;
        }
        height += DOCK_GAP * (this.entries.size() + 1);
        return height;
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return new Size(DOCK_WIDTH, this.dockHeight());
    }
}
