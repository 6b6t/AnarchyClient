package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.DOCK_WIDTH;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.drawText;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textWidth;
import static net.blockhost.anarchyclient.ui.PanelWidgets.wrapText;

import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.rivet.BackgroundDesign;
import net.blockhost.anarchyclient.rivet.SoftShadowCommand;
import net.blockhost.anarchyclient.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayout;
import net.lenni0451.rivet.layout.absolute.AbsoluteOptions;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.minecraft.client.Minecraft;

/**
 * Liquid glass client menu: floating glass islands over the visible game. A vertical icon dock on the
 * left switches between module categories and the Friends / Profiles / Theme tabs; modules are shown
 * as a card grid inside a glass panel; a floating glass inspector edits the opened module.
 */
public final class ModulePanel extends Container implements LayoutDebugLabel {

    private static final float PANEL_MARGIN = 20F;
    private static final float PANEL_GAP = 12F;
    private static final float MODULES_PANEL_WIDTH = 336F;
    private static final float TAB_PANEL_WIDTH = 420F;
    private static final float INSPECTOR_WIDTH = 288F;
    private static final float INSPECTOR_WIDE_WIDTH = 324F;
    private static final float DRAWER_WIDTH = 280F;
    private static final Color SHADOW = Color.fromRGBA(0, 0, 0, 110);
    private static final float SHADOW_SPREAD = 10F;
    private static final float SHADOW_OFFSET_Y = 3F;

    final ModuleManager modules;
    final ClientConfig config;
    private final Dock dock;
    final ModulesPanel modulesPanel;
    final ModuleInspector inspector;
    final FriendsPanel friendsPanel;
    final ProfilesPanel profilesPanel;
    private final ThemePanel themePanel;
    private final SettingsDrawer settingsDrawer;
    private final BrandBlock brand = new BrandBlock();

    Tab selectedTab = Tab.MODULES;
    ModuleCategory selectedCategory;
    Module inspectedModule;
    private Module keybindListening;
    Drawer drawer = Drawer.NONE;
    String searchQuery = "";
    boolean showDisabledModules = true;
    boolean enabledFirst;
    boolean showSummaries = true;
    boolean compactRows;
    boolean wideInspector;
    BackgroundDesign backgroundDesign = BackgroundDesign.NONE;

    private final List<PanelRect> shadowRects = new ArrayList<>();
    Module hoveredModule;
    private float mouseX;
    private float mouseY;

    public ModulePanel(final ModuleManager modules, final ClientConfig config) {
        super(AbsoluteLayout.INSTANCE);
        this.modules = modules;
        this.config = config;
        this.loadUiPreferences(config.uiPreferences());
        this.dock = new Dock(this);
        this.modulesPanel = new ModulesPanel(this);
        this.inspector = new ModuleInspector(this);
        this.friendsPanel = new FriendsPanel(this);
        this.profilesPanel = new ProfilesPanel(this);
        this.themePanel = new ThemePanel(this);
        this.settingsDrawer = new SettingsDrawer(this);
        this.selectedCategory = this.initialCategory();
        this.inspectedModule = null;
        this.addChild(this.dock);
        this.addChild(this.modulesPanel);
        this.addChild(this.inspector);
        this.addChild(this.friendsPanel);
        this.addChild(this.profilesPanel);
        this.addChild(this.themePanel);
        this.addChild(this.settingsDrawer);
        this.addChild(this.brand);
        this.refreshModuleList();
        this.inspector.refresh();
        this.friendsPanel.refresh();
        this.profilesPanel.refresh();
        this.themePanel.refresh();
    }

    private void loadUiPreferences(final ClientConfig.UiPreferences preferences) {
        this.showDisabledModules = preferences.showDisabledModules();
        this.enabledFirst = preferences.enabledModulesFirst();
        this.showSummaries = preferences.showModuleSummaries();
        this.compactRows = preferences.compactRows();
        this.wideInspector = preferences.wideInspector();
        this.backgroundDesign = preferences.backgroundDesign();
        GlassTheme.load(preferences);
    }

    void saveUiPreferences() {
        this.applyUiPreferences();
        this.config.save();
    }

    /**
     * Applies the preferences now but saves them once input settles. Sliders call this on every drag
     * step, so an immediate save would write the file every frame.
     */
    void saveUiPreferencesLater() {
        this.applyUiPreferences();
        this.config.markDirty();
    }

    private void applyUiPreferences() {
        this.config.uiPreferences(new ClientConfig.UiPreferences(
                this.showDisabledModules,
                this.enabledFirst,
                this.showSummaries,
                this.compactRows,
                this.wideInspector,
                GlassTheme.preset(),
                this.backgroundDesign,
                GlassTheme.glassOpacity(),
                GlassTheme.cornerRadius(),
                GlassTheme.glassBlur()));
    }

    void resetUiPreferences() {
        this.loadUiPreferences(ClientConfig.UiPreferences.DEFAULT);
        this.saveUiPreferences();
        this.refreshModuleList();
        this.inspector.refresh();
        this.themePanel.refresh();
        this.applyRivetTheme();
    }

    private void applyRivetTheme() {
        if (this.rivet() != null) {
            this.rivet().theme(new AnarchyClientTheme(GlassTheme.preset()));
        }
        this.configureScroll(this.modulesPanel.scroll);
        this.requestFrame();
    }

    /** Fed by the screen each frame (virtual coordinates); anchors the hover tooltip. */
    public void mousePosition(final float x, final float y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    /** Opens the drag-and-drop HUD editor (no module list; placement only). */
    void openHudEditor() {
        Minecraft.getInstance().gui.setScreen(new HudEditorScreen(this.modules, this.config));
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        // Soft drop shadows under the floating glass islands; everything else is child components.
        for (PanelRect rect : this.shadowRects) {
            renderer.custom(new SoftShadowCommand(
                    rect.x(),
                    rect.y(),
                    rect.width(),
                    rect.height(),
                    rect.radius(),
                    SHADOW_SPREAD,
                    SHADOW_OFFSET_Y,
                    SHADOW));
        }
        super.render(renderer, size);
        this.renderModuleTooltip(renderer, size);
    }

    /**
     * Module descriptions live in a mouse-anchored tooltip (and the inspector header) instead of
     * being crammed into the cards.
     */
    private void renderModuleTooltip(final Renderer renderer, final Size size) {
        if (!this.showSummaries
                || this.hoveredModule == null
                || this.selectedTab != Tab.MODULES
                || this.rivet() == null) {
            return;
        }
        String text = this.hoveredModule.description();
        if (text.isBlank()) {
            return;
        }
        List<String> lines = wrapText(this, text, TEXT, 220F, Integer.MAX_VALUE);
        if (lines.isEmpty()) {
            return;
        }
        float fontHeight = this.rivet().backend().font().height();
        float lineHeight = fontHeight + 2F;
        float textWidth = 0F;
        for (String line : lines) {
            textWidth = Math.max(textWidth, textWidth(this, line, TEXT));
        }
        float width = textWidth + 16F;
        float height = lines.size() * lineHeight - 2F + 12F;
        float x = clamp(this.mouseX + 12F, 4F, Math.max(4F, size.width() - width - 4F));
        float y = clamp(this.mouseY + 14F, 4F, Math.max(4F, size.height() - height - 4F));
        renderer.custom(new SoftShadowCommand(x, y, width, height, 8F, 6F, 2F, SHADOW));
        renderer.optimizedFillRoundedRect(x, y, width, height, 8F, Color.fromRGBA(14, 17, 25, 242));
        float lineY = y + 6F + fontHeight / 2F;
        for (String line : lines) {
            drawText(
                    this,
                    renderer,
                    line,
                    MUTED,
                    x + 8F,
                    lineY,
                    TextOrigin.Horizontal.LOGICAL_LEFT,
                    TextOrigin.Vertical.LOGICAL_CENTER);
            lineY += lineHeight;
        }
    }

    @Override
    public void computeLayout(final Size size) {
        this.shadowRects.clear();
        float panelRadius = GlassTheme.cornerRadius();

        float dockHeight = this.dock.dockHeight();
        float dockX = 12F;
        float dockY = Math.max(10F, (size.height() - dockHeight) / 2F);
        this.dock.layoutOptions(new AbsoluteOptions(dockX, dockY, DOCK_WIDTH, dockHeight));
        this.shadowRects.add(new PanelRect(dockX, dockY, DOCK_WIDTH, dockHeight, DOCK_WIDTH / 2F));

        float contentX = dockX + DOCK_WIDTH + PANEL_GAP;
        float panelY = PANEL_MARGIN;
        float panelHeight = Math.max(180F, size.height() - PANEL_MARGIN * 2F);

        boolean drawerOpen = this.drawer != Drawer.NONE;
        float drawerWidth = Math.min(DRAWER_WIDTH, size.width() - contentX - 24F);
        float reservedRight = drawerOpen ? drawerWidth + PANEL_GAP : 0F;

        if (this.selectedTab == null) {
            // Everything closed: only the dock (and possibly the drawer) floats over the game.
            hide(this.modulesPanel);
            hide(this.inspector);
            hide(this.friendsPanel);
            hide(this.profilesPanel);
            hide(this.themePanel);
        } else if (this.selectedTab == Tab.MODULES) {
            float inspectorWidth = this.wideInspector ? INSPECTOR_WIDE_WIDTH : INSPECTOR_WIDTH;
            boolean inspectorOpen = this.inspectedModule != null;
            float available = size.width() - contentX - 16F - reservedRight;
            float panelWidth =
                    Math.min(MODULES_PANEL_WIDTH, inspectorOpen ? available - inspectorWidth - PANEL_GAP : available);
            panelWidth = Math.max(220F, panelWidth);
            this.modulesPanel.layoutOptions(new AbsoluteOptions(contentX, panelY, panelWidth, panelHeight));
            this.shadowRects.add(new PanelRect(contentX, panelY, panelWidth, panelHeight, panelRadius));
            if (inspectorOpen) {
                float inspectorX = Math.min(
                        contentX + panelWidth + PANEL_GAP, size.width() - inspectorWidth - 16F - reservedRight);
                this.inspector.layoutOptions(new AbsoluteOptions(inspectorX, panelY, inspectorWidth, panelHeight));
                this.shadowRects.add(new PanelRect(inspectorX, panelY, inspectorWidth, panelHeight, panelRadius));
            } else {
                hide(this.inspector);
            }
            hide(this.friendsPanel);
            hide(this.profilesPanel);
            hide(this.themePanel);
        } else {
            Component panel = switch (this.selectedTab) {
                case FRIENDS -> this.friendsPanel;
                case PROFILES -> this.profilesPanel;
                default -> this.themePanel;
            };
            float panelWidth = Math.max(240F, Math.min(TAB_PANEL_WIDTH, size.width() - contentX - 16F - reservedRight));
            panel.layoutOptions(new AbsoluteOptions(contentX, panelY, panelWidth, panelHeight));
            this.shadowRects.add(new PanelRect(contentX, panelY, panelWidth, panelHeight, panelRadius));
            hide(this.modulesPanel);
            hide(this.inspector);
            if (panel != this.friendsPanel) {
                hide(this.friendsPanel);
            }
            if (panel != this.profilesPanel) {
                hide(this.profilesPanel);
            }
            if (panel != this.themePanel) {
                hide(this.themePanel);
            }
        }

        if (drawerOpen) {
            float drawerX = size.width() - drawerWidth - 16F;
            this.settingsDrawer.layoutOptions(new AbsoluteOptions(drawerX, panelY, drawerWidth, panelHeight));
            this.shadowRects.add(new PanelRect(drawerX, panelY, drawerWidth, panelHeight, panelRadius));
        } else {
            hide(this.settingsDrawer);
        }

        this.brand.layoutOptions(new AbsoluteOptions(size.width() - 156F, 8F, 148F, 16F));
        super.computeLayout(size);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return constraints;
    }

    @Override
    public String layoutDebugLabel() {
        String category = this.selectedCategory == null
                ? "none"
                : this.selectedCategory.name().toLowerCase(Locale.ROOT);
        String module = this.inspectedModule == null ? "none" : this.inspectedModule.id();
        return "tab="
                + (this.selectedTab == null ? "none" : this.selectedTab.name().toLowerCase(Locale.ROOT))
                + " category=" + category
                + " module=" + module
                + " drawer=" + this.drawer.name().toLowerCase(Locale.ROOT);
    }

    private static void hide(final Component component) {
        component.layoutOptions(new AbsoluteOptions(0F, 0F, 0F, 0F));
    }

    void selectTab(final Tab tab) {
        this.stopKeybindListening();
        if (this.selectedTab == tab) {
            // Clicking the open dock entry again closes its panel.
            this.selectedTab = null;
            this.requestFrame();
            return;
        }
        this.selectedTab = tab;
        if (tab == Tab.FRIENDS) {
            this.friendsPanel.refresh();
        } else if (tab == Tab.PROFILES) {
            this.profilesPanel.refresh();
        } else if (tab == Tab.THEME) {
            this.themePanel.refresh();
        }
        this.requestFrame();
    }

    void selectCategory(final ModuleCategory category) {
        this.stopKeybindListening();
        if (this.selectedTab == Tab.MODULES && this.selectedCategory == category) {
            // Clicking the open category again closes the modules panel.
            this.selectedTab = null;
            this.requestFrame();
            return;
        }
        this.selectedTab = Tab.MODULES;
        if (this.selectedCategory != category) {
            this.selectedCategory = category;
            this.config.selectedCategory(category);
            this.refreshModuleList();
        }
        this.requestFrame();
    }

    void inspectModule(final Module module) {
        this.inspectedModule = module;
        this.keybindListening = null;
        if (module != null) {
            this.config.selectedModuleId(module.id());
        }
        this.inspector.refresh();
        this.requestFrame();
    }

    void closeInspector() {
        this.inspectedModule = null;
        this.keybindListening = null;
        this.requestFrame();
    }

    /** Cancels a pending keybind capture, so a key pressed after switching tabs is not bound. */
    private void stopKeybindListening() {
        if (this.keybindListening != null) {
            this.keybindListening = null;
            this.inspector.refresh();
        }
    }

    /** True while a keybind row is waiting for the next key press. */
    boolean isCapturingKeybind() {
        return this.keybindListening != null;
    }

    void startKeybindListening(final Module module) {
        this.keybindListening = module;
        this.inspector.refresh();
        this.requestFrame();
    }

    /**
     * Consumes the next key press for the listening keybind row: Escape clears the bind, any other key
     * binds it. Called by the screen before its own Escape handling.
     */
    boolean handleKeybindCapture(final int scancode) {
        Module module = this.keybindListening;
        this.keybindListening = null;
        if (module == null) {
            return false;
        }
        module.keybind().key(scancode == InputConstants.KEY_ESCAPE ? InputConstants.UNKNOWN.getValue() : scancode);
        this.config.save();
        this.inspector.refresh();
        this.requestFrame();
        return true;
    }

    boolean isListeningFor(final Module module) {
        return this.keybindListening == module;
    }

    void toggleModule(final Module module) {
        module.toggle();
        this.config.save();
        if (this.enabledFirst) {
            this.refreshModuleList();
        }
        this.inspector.refresh();
        this.requestFrame();
    }

    void toggleFavorite(final Module module) {
        this.config.moduleFavorite(module.id(), !this.config.moduleFavorite(module.id()));
        this.config.save();
        this.requestFrame();
    }

    void openDrawer(final Drawer drawer) {
        this.drawer = this.drawer == drawer ? Drawer.NONE : drawer;
        this.settingsDrawer.refresh();
        this.requestFrame();
    }

    void openRootDrawer() {
        this.drawer = Drawer.ROOT;
        this.settingsDrawer.refresh();
        this.requestFrame();
    }

    void saveConfigNow() {
        this.saveUiPreferences();
        this.config.save();
    }

    void reloadConfig() {
        this.config.load();
        this.loadUiPreferences(this.config.uiPreferences());
        this.selectedCategory = this.initialCategory();
        this.refreshModuleList();
        this.inspector.refresh();
        this.friendsPanel.refresh();
        this.profilesPanel.refresh();
        this.themePanel.refresh();
        this.applyRivetTheme();
    }

    void resetModuleListPreferences() {
        this.showDisabledModules = ClientConfig.UiPreferences.DEFAULT.showDisabledModules();
        this.enabledFirst = ClientConfig.UiPreferences.DEFAULT.enabledModulesFirst();
        this.showSummaries = ClientConfig.UiPreferences.DEFAULT.showModuleSummaries();
        this.searchQuery = "";
        this.modulesPanel.search.text("");
        this.saveUiPreferences();
        this.refreshModuleList();
    }

    void selectThemePreset(final ClientConfig.GuiThemePreset preset) {
        GlassTheme.preset(preset);
        this.saveUiPreferences();
        this.applyRivetTheme();
        this.themePanel.refresh();
    }

    void cycleBackgroundDesign() {
        BackgroundDesign[] designs = BackgroundDesign.values();
        this.backgroundDesign = designs[(this.backgroundDesign.ordinal() + 1) % designs.length];
        this.saveUiPreferences();
        this.requestFrame();
    }

    void selectModuleShortcut(final Module module) {
        this.selectedTab = Tab.MODULES;
        this.drawer = Drawer.NONE;
        this.selectedCategory = module.category();
        this.config.selectedCategory(this.selectedCategory);
        this.refreshModuleList();
        this.inspectModule(module);
    }

    private Module module(final String id) {
        return this.modules.find(id).orElse(null);
    }

    boolean addModuleShortcut(final Container container, final String moduleId) {
        Module module = this.module(moduleId);
        if (module != null) {
            container.addChild(new ModuleShortcutRow(this, module));
            return true;
        }
        return false;
    }

    void dumpLayoutTree() {
        if (this.rivet() == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Path directory = FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + "-debug");
        try {
            Path file = LayoutTreeDumper.writeSnapshot(this.rivet(), directory);
            AnarchyClient.LOGGER.info("Layout tree snapshot saved to {}", file);
            if (client.player != null) {
                client.player.sendSystemMessage(Translations.component(
                        "anarchyclient.message.module_panel.layout_tree_saved_to", "Layout tree saved to %s.", file));
            }
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to save layout tree snapshot", exception);
            if (client.player != null) {
                client.player.sendSystemMessage(Translations.component(
                        "anarchyclient.message.module_panel.failed_to_save_layout_tree_snapshot",
                        "Failed to save layout tree snapshot."));
            }
        }
    }

    void refreshModuleList() {
        this.modulesPanel.refresh();
        this.requestFrame();
    }

    List<Module> visibleModules() {
        String query = this.searchQuery.toLowerCase(Locale.ROOT);
        List<Module> result = query.isEmpty()
                ? new ArrayList<>(this.modules.byCategory(this.selectedCategory))
                : new ArrayList<>(this.modules.all());
        result.removeIf(module -> {
            if (!this.showDisabledModules && !module.enabled()) {
                return true;
            }
            return !query.isEmpty() && !matchesQuery(module, query);
        });
        if (!query.isEmpty()) {
            result.sort(
                    Comparator.comparing((Module module) -> module.category().ordinal())
                            .thenComparing(Module::name, String.CASE_INSENSITIVE_ORDER));
        }
        if (this.enabledFirst) {
            result.sort(Comparator.comparing((Module module) -> Boolean.valueOf(module.enabled()))
                    .reversed()
                    .thenComparing(Module::name, String.CASE_INSENSITIVE_ORDER));
        }
        return result;
    }

    private ModuleCategory initialCategory() {
        ModuleCategory fallback = this.firstNonEmptyCategory();
        return this.config
                .selectedCategory()
                .filter(category -> !this.modules.byCategory(category).isEmpty())
                .orElse(fallback);
    }

    private ModuleCategory firstNonEmptyCategory() {
        for (ModuleCategory category : ModuleCategory.values()) {
            if (!this.modules.byCategory(category).isEmpty()) {
                return category;
            }
        }
        return ModuleCategory.COMBAT;
    }

    void configureScroll(final ScrollContainer scroll) {
        scroll.barColor().set(Color.fromRGBA(255, 255, 255, 46));
        scroll.barHoverColor().set(Color.fromRGBA(255, 255, 255, 84));
        // A supplier, so a new accent applies without rebuilding the panel.
        scroll.barClickColor().set(() -> GlassTheme.accent().multiplyAlpha(0.72F));
        scroll.scrollSpeed().set(20F);
        scroll.barType().set(ScrollContainer.ScrollBarType.FLOATING);
    }

    void requestFrame() {
        if (this.parent() != null) {
            this.parent().requestLayoutRecalculation();
        }
        if (this.rivet() != null) {
            this.rivet().recalculateNextFrame();
        }
    }

    int enabledCount(final ModuleCategory category) {
        int count = 0;
        for (Module module : this.modules.byCategory(category)) {
            if (module.enabled()) {
                count++;
            }
        }
        return count;
    }

    private static boolean matchesQuery(final Module module, final String query) {
        if (module.name().toLowerCase(Locale.ROOT).contains(query)
                || module.id().contains(query)
                || module.description().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        for (String alias : module.aliases()) {
            if (alias.toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        for (Setting<?> setting : module.settings()) {
            if (setting.id().contains(query)
                    || setting.name().toLowerCase(Locale.ROOT).contains(query)
                    || setting.description().toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private static float clamp(final float value, final float min, final float max) {
        return (float) MathUtils.clamp(value, min, max);
    }

    private static GridCell weightedCell(final int column, final int row, final float weightX, final float weightY) {
        return cell(column, row, 1, 1, weightX, weightY, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, null);
    }

    private static GridCell weightedCell(
            final int column,
            final int row,
            final int columnSpan,
            final int rowSpan,
            final float weightX,
            final float weightY) {
        return cell(
                column,
                row,
                columnSpan,
                rowSpan,
                weightX,
                weightY,
                GridAnchor.CENTER,
                GridFill.BOTH,
                Padding.EMPTY,
                null,
                null);
    }

    GlassSurface glassPanel() {
        return new GlassSurface(GlassTheme::glass, GlassTheme::cornerRadius, () -> this.backgroundDesign);
    }

    GlassSurface glassDeepPanel() {
        return new GlassSurface(GlassTheme::glassDeep, GlassTheme::cornerRadius, () -> this.backgroundDesign);
    }
}
