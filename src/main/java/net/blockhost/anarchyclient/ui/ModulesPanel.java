package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.HEADER_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BUTTON_SIZE;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.SEARCH_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.fillCell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.iconButton;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

import java.util.List;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

final class ModulesPanel extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    final SearchInput search;
    // HUD editor entry point: shown top-right only on the HUD tab. Positioning moved out of the
    // per-module settings into a drag editor, so this button is how you reach it.
    final CardGrid cards;
    final ScrollContainer scroll;

    ModulesPanel(final ModulePanel panel) {
        super(gridLayout(0, 0));
        this.panel = panel;
        GlassSurface background = this.panel.glassPanel();
        TextNode title = textNode(this::title, () -> TEXT);
        TextNode count = textNode(this::countText, GlassTheme::accent)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        this.search =
                new SearchInput(Translations.text("anarchyclient.ui.search_all_modules", "Search all modules..."));
        IconButton editHud = iconButton("move", GlassTheme::accent, this.panel::openHudEditor, this::showEditHud);
        this.cards = new CardGrid(this.panel);
        this.scroll = new LayoutDebugScrollContainer(this.cards);
        this.panel.configureScroll(this.scroll);
        this.search.onChange(value -> {
            this.panel.searchQuery = value.trim();
            this.panel.refreshModuleList();
        });
        place(background, fillCell(0, 0, 2, 3));
        place(
                title,
                cell(
                        0,
                        0,
                        1,
                        1,
                        1F,
                        0F,
                        GridAnchor.CENTER,
                        GridFill.BOTH,
                        padding(PADDING + 2F, 6F, 8F, 0F),
                        null,
                        HEADER_HEIGHT));
        place(
                count,
                cell(
                        1,
                        0,
                        1,
                        1,
                        0F,
                        0F,
                        GridAnchor.CENTER,
                        GridFill.BOTH,
                        padding(0F, 6F, PADDING + 2F, 0F),
                        52F,
                        HEADER_HEIGHT));
        place(
                editHud,
                cell(
                        1,
                        0,
                        1,
                        1,
                        0F,
                        0F,
                        GridAnchor.RIGHT,
                        GridFill.NONE,
                        padding(0F, 6F, PADDING, 0F),
                        ICON_BUTTON_SIZE,
                        ICON_BUTTON_SIZE));
        place(
                this.search,
                cell(
                        0,
                        1,
                        2,
                        1,
                        1F,
                        0F,
                        GridAnchor.CENTER,
                        GridFill.BOTH,
                        padding(PADDING, 0F, PADDING, 6F),
                        null,
                        SEARCH_HEIGHT));
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
        this.addChild(background);
        this.addChild(title);
        this.addChild(count);
        this.addChild(editHud);
        this.addChild(this.search);
        this.addChild(this.scroll);
    }

    private boolean showEditHud() {
        return this.panel.selectedTab == Tab.MODULES
                && this.panel.selectedCategory == ModuleCategory.HUD
                && this.panel.searchQuery.isEmpty();
    }

    void refresh() {
        this.cards.clearChildren();
        List<Module> visible = this.panel.visibleModules();
        if (visible.isEmpty()) {
            this.cards.addChild(new EmptyState(Translations.text(
                    "anarchyclient.ui.no_modules_match_the_current_filter", "No modules match the current filter.")));
        } else {
            for (Module module : visible) {
                this.cards.addChild(new ModuleCard(this.panel, module));
            }
        }
    }

    private String title() {
        if (!this.panel.searchQuery.isEmpty()) {
            return Translations.text("anarchyclient.ui.search", "Search");
        }
        return this.panel.selectedCategory == null
                ? Translations.text("anarchyclient.ui.modules", "Modules")
                : this.panel.selectedCategory.displayName();
    }

    private String countText() {
        // The HUD tab shows the editor button here instead of a count.
        if (this.showEditHud() || !this.panel.searchQuery.isEmpty() || this.panel.selectedCategory == null) {
            return "";
        }
        int count = this.panel.enabledCount(this.panel.selectedCategory);
        return count > 0 ? Translations.format("anarchyclient.ui.enabled_count", "%s on", count) : "";
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return constraints;
    }

    @Override
    public String layoutDebugLabel() {
        return title();
    }
}
