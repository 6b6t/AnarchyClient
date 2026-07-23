package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.command.CommandPrefix;
import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.notification.ToggleNotifications;
import net.blockhost.anarchyclient.profile.ProfileManager;
import net.blockhost.anarchyclient.rivet.BackgroundDesign;
import net.blockhost.anarchyclient.rivet.Blaze3DRenderCommand;
import net.blockhost.anarchyclient.rivet.GlassPanelCommand;
import net.blockhost.anarchyclient.rivet.RivetInputMapper;
import net.blockhost.anarchyclient.rivet.SoftShadowCommand;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.SettingGroup;
import net.blockhost.anarchyclient.setting.TextValueSetting;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.component.impl.FormattedLabel;
import net.lenni0451.rivet.component.impl.TextField;
import net.lenni0451.rivet.component.impl.slider.Slider;
import net.lenni0451.rivet.input.keyboard.Key;
import net.lenni0451.rivet.input.mouse.ClickOn;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayout;
import net.lenni0451.rivet.layout.absolute.AbsoluteOptions;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.layout.grid.GridLayout;
import net.lenni0451.rivet.layout.grid.GridOptions;
import net.lenni0451.rivet.layout.list.VerticalListLayout;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextFormat;
import net.lenni0451.rivet.text.model.TextLine;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.text.model.TextSection;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceExtractor;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Liquid glass client menu: floating glass islands over the visible game. A vertical icon dock on the
 * left switches between module categories and the Friends / Profiles / Theme tabs; modules are shown
 * as a card grid inside a glass panel; a floating glass inspector edits the opened module.
 */
public final class ModulePanel extends Container implements LayoutDebugLabel {

    private static final float DOCK_WIDTH = 46F;
    private static final float DOCK_BUTTON = 30F;
    private static final float DOCK_GAP = 2F;
    private static final float DOCK_DIVIDER = 7F;
    private static final float PANEL_MARGIN = 20F;
    private static final float PANEL_GAP = 12F;
    private static final float MODULES_PANEL_WIDTH = 336F;
    private static final float TAB_PANEL_WIDTH = 420F;
    private static final float INSPECTOR_WIDTH = 288F;
    private static final float INSPECTOR_WIDE_WIDTH = 324F;
    private static final float DRAWER_WIDTH = 280F;
    private static final float PADDING = 12F;
    private static final float SEARCH_HEIGHT = 28F;
    private static final float HEADER_HEIGHT = 36F;
    private static final float CARD_GAP = 8F;
    private static final float SETTING_ROW_HEIGHT = 46F;
    private static final float NUMBER_ROW_HEIGHT = 64F;
    private static final float TEXT_ROW_HEIGHT = 54F;
    private static final float OPTION_ROW_HEIGHT = 34F;
    private static final float INSPECTOR_HEADER_HEIGHT = 96F;
    // The switch box is 2px taller than the painted track so the SDF anti-aliased edge and
    // half-pixel snapping can never clip the pill at the bottom.
    private static final float SWITCH_WIDTH = 24F;
    private static final float SWITCH_HEIGHT = 14F;
    private static final float SWITCH_TRACK_HEIGHT = 12F;
    private static final float ICON_BOX = 20F;
    private static final float ICON_BUTTON_SIZE = 28F;
    // The Lucide TTF rasterizes as a 20px icon font in Minecraft. Keep the
    // component box at that size so Rivet's component scissor does not crop it,
    // and calibrate the font's y anchor once inside IconNode.
    private static final float ICON_TEXT_Y_FROM_CENTER = 2F;

    private static final Color TEXT = GlassTheme.TEXT;
    private static final Color MUTED = GlassTheme.MUTED;
    private static final Color FAINT = GlassTheme.FAINT;
    private static final Color DIVIDER = GlassTheme.DIVIDER;
    private static final Color CARD = GlassTheme.CARD;
    private static final Color CARD_HOVER = GlassTheme.CARD_HOVER;
    private static final Color FIELD = GlassTheme.FIELD;
    private static final Color TRACK = GlassTheme.TRACK;
    private static final Color WARNING = GlassTheme.WARNING;
    private static final Color SHADOW = Color.fromRGBA(0, 0, 0, 110);
    private static final float SHADOW_SPREAD = 10F;
    private static final float SHADOW_OFFSET_Y = 3F;
    private static final FontDescription LUCIDE_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "lucide")
    );

    private final ModuleManager modules;
    private final ClientConfig config;
    private final Dock dock;
    private final ModulesPanel modulesPanel;
    private final ModuleInspector inspector;
    private final FriendsPanel friendsPanel;
    private final ProfilesPanel profilesPanel;
    private final ThemePanel themePanel;
    private final SettingsDrawer settingsDrawer;
    private final BrandBlock brand = new BrandBlock();

    private Tab selectedTab = Tab.MODULES;
    private ModuleCategory selectedCategory;
    private Module inspectedModule;
    private Module keybindListening;
    private Drawer drawer = Drawer.NONE;
    private String searchQuery = "";
    private boolean showDisabledModules = true;
    private boolean enabledFirst;
    private boolean showSummaries = true;
    private boolean compactRows;
    private boolean wideInspector;
    private BackgroundDesign backgroundDesign = BackgroundDesign.NONE;

    private final List<PanelRect> shadowRects = new ArrayList<>();
    private Module hoveredModule;
    private float mouseX;
    private float mouseY;

    public ModulePanel(final ModuleManager modules, final ClientConfig config) {
        super(AbsoluteLayout.INSTANCE);
        this.modules = modules;
        this.config = config;
        this.loadUiPreferences(config.uiPreferences());
        this.dock = new Dock();
        this.modulesPanel = new ModulesPanel();
        this.inspector = new ModuleInspector();
        this.friendsPanel = new FriendsPanel();
        this.profilesPanel = new ProfilesPanel();
        this.themePanel = new ThemePanel();
        this.settingsDrawer = new SettingsDrawer();
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

    private void saveUiPreferences() {
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
                GlassTheme.glassBlur()
        ));
        this.config.save();
    }

    private void resetUiPreferences() {
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
    private void openHudEditor() {
        Minecraft.getInstance().gui.setScreen(new HudEditorScreen(this.modules, this.config));
    }

    @Override
    public void render(final Renderer renderer, final Size size) {
        // Soft drop shadows under the floating glass islands; everything else is child components.
        for (PanelRect rect : this.shadowRects) {
            renderer.custom(new SoftShadowCommand(rect.x(), rect.y(), rect.width(), rect.height(),
                    rect.radius(), SHADOW_SPREAD, SHADOW_OFFSET_Y, SHADOW));
        }
        super.render(renderer, size);
        this.renderModuleTooltip(renderer, size);
    }

    /**
     * Module descriptions live in a mouse-anchored tooltip (and the inspector header) instead of
     * being crammed into the cards.
     */
    private void renderModuleTooltip(final Renderer renderer, final Size size) {
        if (!this.showSummaries || this.hoveredModule == null || this.selectedTab != Tab.MODULES || this.rivet() == null) {
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
            drawText(this, renderer, line, MUTED, x + 8F, lineY,
                    TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
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
            float panelWidth = Math.min(MODULES_PANEL_WIDTH, inspectorOpen ? available - inspectorWidth - PANEL_GAP : available);
            panelWidth = Math.max(220F, panelWidth);
            this.modulesPanel.layoutOptions(new AbsoluteOptions(contentX, panelY, panelWidth, panelHeight));
            this.shadowRects.add(new PanelRect(contentX, panelY, panelWidth, panelHeight, panelRadius));
            if (inspectorOpen) {
                float inspectorX = Math.min(contentX + panelWidth + PANEL_GAP, size.width() - inspectorWidth - 16F - reservedRight);
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
        String category = this.selectedCategory == null ? "none" : this.selectedCategory.name().toLowerCase(Locale.ROOT);
        String module = this.inspectedModule == null ? "none" : this.inspectedModule.id();
        return "tab=" + (this.selectedTab == null ? "none" : this.selectedTab.name().toLowerCase(Locale.ROOT))
                + " category=" + category
                + " module=" + module
                + " drawer=" + this.drawer.name().toLowerCase(Locale.ROOT);
    }

    private record PanelRect(float x, float y, float width, float height, float radius) {
    }

    private static void hide(final Component component) {
        component.layoutOptions(new AbsoluteOptions(0F, 0F, 0F, 0F));
    }

    private void selectTab(final Tab tab) {
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

    private void selectCategory(final ModuleCategory category) {
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

    private void inspectModule(final Module module) {
        this.inspectedModule = module;
        this.keybindListening = null;
        if (module != null) {
            this.config.selectedModuleId(module.id());
        }
        this.inspector.refresh();
        this.requestFrame();
    }

    private void closeInspector() {
        this.inspectedModule = null;
        this.keybindListening = null;
        this.requestFrame();
    }

    /** True while a keybind row is waiting for the next key press. */
    boolean isCapturingKeybind() {
        return this.keybindListening != null;
    }

    private void startKeybindListening(final Module module) {
        this.keybindListening = module;
        this.inspector.refresh();
        this.requestFrame();
    }

    /**
     * Consumes the next key press for the listening keybind row: Escape clears the bind, any other key
     * binds it. Called by the screen before its own Escape handling.
     */
    boolean handleKeybindCapture(final int glfwKey) {
        Module module = this.keybindListening;
        this.keybindListening = null;
        if (module == null) {
            return false;
        }
        module.keybind().key(glfwKey == GLFW.GLFW_KEY_ESCAPE ? GLFW.GLFW_KEY_UNKNOWN : glfwKey);
        this.config.save();
        this.inspector.refresh();
        this.requestFrame();
        return true;
    }

    private boolean isListeningFor(final Module module) {
        return this.keybindListening == module;
    }

    private static String keyLabel(final int glfwKey) {
        if (glfwKey == GLFW.GLFW_KEY_UNKNOWN) {
            return "None";
        }
        return RivetInputMapper.fromGlfw(glfwKey).map(Key::name).orElse("Key " + glfwKey);
    }

    private void toggleModule(final Module module) {
        module.toggle();
        this.config.save();
        if (this.enabledFirst) {
            this.refreshModuleList();
        }
        this.inspector.refresh();
        this.requestFrame();
    }

    private void toggleFavorite(final Module module) {
        this.config.moduleFavorite(module.id(), !this.config.moduleFavorite(module.id()));
        this.config.save();
        this.requestFrame();
    }

    private void openDrawer(final Drawer drawer) {
        this.drawer = this.drawer == drawer ? Drawer.NONE : drawer;
        this.settingsDrawer.refresh();
        this.requestFrame();
    }

    private void openRootDrawer() {
        this.drawer = Drawer.ROOT;
        this.settingsDrawer.refresh();
        this.requestFrame();
    }

    private void saveConfigNow() {
        this.saveUiPreferences();
        this.config.save();
    }

    private void reloadConfig() {
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

    private void resetModuleListPreferences() {
        this.showDisabledModules = ClientConfig.UiPreferences.DEFAULT.showDisabledModules();
        this.enabledFirst = ClientConfig.UiPreferences.DEFAULT.enabledModulesFirst();
        this.showSummaries = ClientConfig.UiPreferences.DEFAULT.showModuleSummaries();
        this.searchQuery = "";
        this.modulesPanel.search.text("");
        this.saveUiPreferences();
        this.refreshModuleList();
    }

    private void selectThemePreset(final ClientConfig.GuiThemePreset preset) {
        GlassTheme.preset(preset);
        this.saveUiPreferences();
        this.applyRivetTheme();
        this.themePanel.refresh();
    }

    private void cycleBackgroundDesign() {
        BackgroundDesign[] designs = BackgroundDesign.values();
        this.backgroundDesign = designs[(this.backgroundDesign.ordinal() + 1) % designs.length];
        this.saveUiPreferences();
        this.requestFrame();
    }

    private void selectModuleShortcut(final Module module) {
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

    private boolean addModuleShortcut(final Container container, final String moduleId) {
        Module module = this.module(moduleId);
        if (module != null) {
            container.addChild(new ModuleShortcutRow(module));
            return true;
        }
        return false;
    }

    private void dumpLayoutTree() {
        if (this.rivet() == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Path directory = FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + "-debug");
        try {
            Path file = LayoutTreeDumper.writeSnapshot(this.rivet(), directory);
            AnarchyClient.LOGGER.info("Layout tree snapshot saved to {}", file);
            if (client.player != null) {
                client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Layout tree saved to " + file + "."));
            }
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to save layout tree snapshot", exception);
            if (client.player != null) {
                client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Failed to save layout tree snapshot."));
            }
        }
    }

    private void refreshModuleList() {
        this.modulesPanel.refresh();
        this.requestFrame();
    }

    private List<Module> visibleModules() {
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
            result.sort(Comparator.comparing((Module module) -> module.category().ordinal())
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
        return this.config.selectedCategory()
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

    private void configureScroll(final ScrollContainer scroll) {
        scroll.barColor().set(Color.fromRGBA(255, 255, 255, 46));
        scroll.barHoverColor().set(Color.fromRGBA(255, 255, 255, 84));
        scroll.barClickColor().set(GlassTheme.accent().multiplyAlpha(0.72F));
        scroll.scrollSpeed().set(20F);
        scroll.barType().set(ScrollContainer.ScrollBarType.FLOATING);
    }

    private void requestFrame() {
        if (this.parent() != null) {
            this.parent().requestLayoutRecalculation();
        }
        if (this.rivet() != null) {
            this.rivet().recalculateNextFrame();
        }
    }

    private int enabledCount(final ModuleCategory category) {
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
                    || module.settingDescription(setting).toLowerCase(Locale.ROOT).contains(query)) {
                return true;
            }
        }
        return false;
    }

    private static List<SettingGroup> visibleSettingGroups(final Module module) {
        List<SettingGroup> groups = new ArrayList<>();
        for (SettingGroup group : module.settingGroups()) {
            if (group.visible()) {
                groups.add(group);
            }
        }
        return groups;
    }

    private static boolean showGroupHeaders(final List<SettingGroup> groups) {
        return groups.size() > 1 || (!groups.isEmpty() && !"general".equals(groups.getFirst().id()));
    }

    private static Component settingRow(final Module module, final Setting<?> setting, final Runnable rebuild, final Runnable save) {
        if (setting instanceof BooleanSetting bool) {
            return new BooleanSettingRow(module, bool, rebuild);
        }
        if (setting instanceof NumberSetting number) {
            return new NumberSettingRow(module, number, save);
        }
        if (setting instanceof SelectSetting select) {
            return new SelectSettingRow(module, select, rebuild);
        }
        if (setting instanceof TextValueSetting text) {
            return new TextSettingRow(module, setting, text, save);
        }
        return new ValueSettingRow(module, setting);
    }

    private static String fitText(final Component component, final String value, final Color color, final float maxWidth) {
        if (maxWidth <= 0F || value.isEmpty()) {
            return "";
        }
        if (textWidth(component, value, color) <= maxWidth) {
            return value;
        }
        String ellipsis = "...";
        if (textWidth(component, ellipsis, color) > maxWidth) {
            return "";
        }
        return value.substring(0, fitLength(component, value, color, maxWidth, ellipsis)).stripTrailing() + ellipsis;
    }

    /** Longest prefix of {@code value} that, with {@code suffix} appended, still fits {@code maxWidth}. */
    private static int fitLength(final Component component, final String value, final Color color,
                                 final float maxWidth, final String suffix) {
        int low = 0;
        int high = value.length();
        while (low < high) {
            int middle = (low + high + 1) / 2;
            String candidate = value.substring(0, middle).stripTrailing() + suffix;
            if (textWidth(component, candidate, color) <= maxWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }

    /**
     * Greedy word wrap: break at spaces while lines fit, ellipsize only the final allowed line.
     */
    private static List<String> wrapText(final Component component, final String value, final Color color,
                                         final float maxWidth, final int maxLines) {
        if (maxWidth <= 0F || value.isBlank()) {
            return List.of();
        }
        List<String> lines = new ArrayList<>(Math.min(maxLines, 8));
        String remaining = value.trim();
        while (!remaining.isEmpty() && lines.size() < maxLines) {
            if (lines.size() == maxLines - 1) {
                lines.add(fitText(component, remaining, color, maxWidth));
                break;
            }
            if (textWidth(component, remaining, color) <= maxWidth) {
                lines.add(remaining);
                break;
            }
            int fit = fitLength(component, remaining, color, maxWidth, "");
            int breakAt = remaining.lastIndexOf(' ', fit);
            if (breakAt <= 0) {
                breakAt = Math.max(1, fit);
            }
            lines.add(remaining.substring(0, breakAt).stripTrailing());
            remaining = remaining.substring(breakAt).stripLeading();
        }
        return lines;
    }

    private static float textWidth(final Component component, final String value, final Color color) {
        return component.rivet().backend().font().shapeText(value, color).logicalBounds().width();
    }

    private static float clamp(final float value, final float min, final float max) {
        return (float) MathUtils.clamp(value, min, max);
    }

    private static GridCell fillCell(final int column, final int row) {
        return cell(column, row, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, null);
    }

    private static GridCell fillCell(final int column, final int row, final int columnSpan, final int rowSpan) {
        return cell(column, row, columnSpan, rowSpan, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, null);
    }

    private static GridCell weightedCell(final int column, final int row, final float weightX, final float weightY) {
        return cell(column, row, 1, 1, weightX, weightY, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, null);
    }

    private static GridCell weightedCell(final int column, final int row, final int columnSpan, final int rowSpan,
                                         final float weightX, final float weightY) {
        return cell(column, row, columnSpan, rowSpan, weightX, weightY, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, null);
    }

    private static GridCell fixedCell(final int column, final int row, final float width, final float height,
                                      final GridAnchor anchor) {
        return cell(column, row, 1, 1, 0F, 0F, anchor, GridFill.NONE, Padding.EMPTY, width, height);
    }

    private static GridCell fixedCell(final int column, final int row, final int columnSpan, final int rowSpan,
                                      final float width, final float height, final GridAnchor anchor) {
        return cell(column, row, columnSpan, rowSpan, 0F, 0F, anchor, GridFill.NONE, Padding.EMPTY, width, height);
    }

    private static GridCell cell(final int column, final int row, final int columnSpan, final int rowSpan,
                                 final float weightX, final float weightY, final GridAnchor anchor,
                                 final GridFill fill, final Padding padding,
                                 final Float width, final Float height) {
        // Rivet's GridLayout uses child ideal sizes as row/column minimums. Flexible cells
        // need explicit zero bases so text and scroll content cannot push fixed UI outside.
        Float effectiveWidth = width;
        if (effectiveWidth == null && weightX > 0F) {
            effectiveWidth = 0F;
        }
        Float effectiveHeight = height;
        if (effectiveHeight == null && weightY > 0F) {
            effectiveHeight = 0F;
        }
        return new GridCell(
                new GridOptions(column, row, columnSpan, rowSpan, weightX, weightY, anchor, fill, padding),
                effectiveWidth,
                effectiveHeight
        );
    }

    private static void place(final Component component, final GridCell cell) {
        GridFill fill = cell.options().fill();
        if (cell.width() != null) {
            if (fill == GridFill.HORIZONTAL || fill == GridFill.BOTH) {
                component.minSize(cell.width(), component.minSize().height());
            } else {
                component.fixedSize(cell.width(), -1);
            }
        }
        if (cell.height() != null) {
            if (fill == GridFill.VERTICAL || fill == GridFill.BOTH) {
                component.minSize(component.minSize().width(), cell.height());
            } else {
                component.fixedSize(-1, cell.height());
            }
        }
        component.layoutOptions(cell.options());
    }

    private static GridLayout gridLayout(final int horizontalGap, final int verticalGap) {
        return new GridLayout(horizontalGap, verticalGap, false, false, true, true);
    }

    private record GridCell(GridOptions options, Float width, Float height) {
    }

    private static Padding padding(final float left, final float top, final float right, final float bottom) {
        return new Padding(left, top, right, bottom);
    }

    private static FormattedLabel label(final String text, final Color color) {
        FormattedLabel label = new FormattedLabel(new TextLine(new TextSection(text, TextFormat.DEFAULT.withColor(color))));
        label.horizontalOrigin(TextOrigin.Horizontal.LOGICAL_LEFT);
        label.verticalOrigin(TextOrigin.Vertical.LOGICAL_CENTER);
        label.capabilities().all(false);
        return label;
    }

    private static Button button(final String text, final Color color, final Button.ClickListener listener) {
        Button button = new Button(label(text, color), listener);
        button.cornerRadius().set(8F);
        button.outlineWidth().set(1F);
        button.inactiveColor().set(CARD);
        button.inactiveOutlineColor().set(DIVIDER);
        button.activeColor().set(CARD_HOVER);
        button.activeOutlineColor().set(Color.fromRGBA(255, 255, 255, 60));
        button.clickColor().set(Color.fromRGBA(255, 255, 255, 10));
        button.clickOutlineColor().set(GlassTheme.accent());
        button.innerPadding().set(new Padding(8, 4, 8, 4));
        button.clickOn().set(ClickOn.UP);
        return button;
    }

    private static TextField textField(final String value) {
        TextField field = new TextField(value);
        field.backgroundColor().set(FIELD);
        field.outlineColor().set(DIVIDER);
        field.focusedOutlineColor().set(GlassTheme.accent());
        field.selectionColor().set(GlassTheme.accent().multiplyAlpha(0.4F));
        field.cursorColor().set(TEXT);
        field.cornerRadius().set(8F);
        field.innerPadding().set(new Padding(7, 4, 7, 4));
        return field;
    }

    /** Runs {@code action} when Enter (or numpad Enter) is pressed while the field is focused. */
    private static void onEnter(final TextField field, final Runnable action) {
        field.keyDownListener().add(event -> {
            if (event.key().isEquivalent(Key.ENTER) || event.key().isEquivalent(Key.KP_ENTER)) {
                action.run();
                return true;
            }
            return false;
        });
    }

    private static Slider slider(final double min, final double max, final double step, final double value) {
        Slider slider = new Slider(min, max, step, value);
        slider.barColor().set(TRACK);
        slider.activeBarColor().set(GlassTheme.accent());
        slider.thumbColor().set(TEXT);
        slider.thumbClickColor().set(GlassTheme.accent());
        slider.barHeight().set(3F);
        slider.barCornerRadius().set(1.5F);
        slider.thumbWidth().set(9F);
        slider.thumbHeight().set(9F);
        slider.thumbCornerRadius().set(4.5F);
        slider.fixedSize(-1, 16);
        return slider;
    }

    private static void drawText(final Component component, final Renderer renderer, final String text, final Color color,
                                 final float x, final float y, final TextOrigin.Horizontal horizontal,
                                 final TextOrigin.Vertical vertical) {
        renderer.text(component.rivet().backend().font().shapeText(text, color), x, y, horizontal, vertical);
    }

    /**
     * Draws a Lucide icon centered on {@code (centerX, centerY)}. The X axis is centered exactly
     * using the measured glyph width; the Y axis uses the single {@link #ICON_TEXT_Y_FROM_CENTER}
     * calibration so every icon sits the same way instead of each call site guessing.
     */
    private static void drawIcon(final Renderer renderer, final String icon, final float centerX, final float centerY, final Color color) {
        String glyph = LucideIcons.glyph(icon);
        if (glyph.isEmpty()) {
            return;
        }
        net.minecraft.network.chat.Component component = net.minecraft.network.chat.Component.literal(glyph)
                .withStyle(style -> style.withFont(LUCIDE_FONT));
        float width = Minecraft.getInstance().font.width(component);
        float left = centerX - width / 2F;
        float top = centerY - ICON_TEXT_Y_FROM_CENTER;
        renderer.custom(new Blaze3DRenderCommand(
                (GuiGraphicsExtractor graphics) -> graphics.text(
                        Minecraft.getInstance().font,
                        component,
                        Math.round(left),
                        Math.round(top),
                        argb(color),
                        false
                ),
                new Rectangle(0F, 0F, ICON_BOX, ICON_BOX)
        ));
    }

    private static void drawFittedText(final Component component, final Renderer renderer, final String text, final Color color,
                                       final float x, final float y, final float maxWidth,
                                       final TextOrigin.Horizontal horizontal, final TextOrigin.Vertical vertical) {
        String fitted = fitText(component, text, color, maxWidth);
        if (!fitted.isEmpty()) {
            drawText(component, renderer, fitted, color, x, y, horizontal, vertical);
        }
    }

    private static TextNode textNode(final String text, final Color color) {
        return new TextNode(() -> text, () -> color);
    }

    private static TextNode textNode(final String text, final Supplier<Color> color) {
        return new TextNode(() -> text, color);
    }

    private static TextNode textNode(final Supplier<String> text, final Supplier<Color> color) {
        return new TextNode(text, color);
    }

    private static IconNode iconNode(final String icon, final Color color) {
        return new IconNode(() -> icon, () -> color);
    }

    private static IconNode iconNode(final String icon, final Supplier<Color> color) {
        return new IconNode(() -> icon, color);
    }

    private static IconNode iconNode(final Supplier<String> icon, final Supplier<Color> color) {
        return new IconNode(icon, color);
    }

    private static IconButton iconButton(final String icon, final Supplier<Color> color, final Runnable action) {
        return iconButton(icon, color, action, () -> true);
    }

    private static IconButton iconButton(final String icon, final Supplier<Color> color, final Runnable action,
                                         final BooleanSupplier visible) {
        return new IconButton(() -> icon, color, action, visible);
    }

    private static TextAction textAction(final String text, final Color color, final Runnable action) {
        return new TextAction(() -> text, () -> color, action);
    }

    private static Surface surface(final Color color) {
        return new Surface(() -> color);
    }

    private static Surface surface(final Supplier<Color> color) {
        return new Surface(color);
    }

    private static String categoryIcon(final ModuleCategory category) {
        return switch (category) {
            case COMBAT -> "crosshair";
            case RENDER -> "eye";
            case MOVEMENT -> "move";
            case WORLD -> "globe";
            case PLAYER -> "user";
            case HUD -> "layout";
            case MISC -> "package";
            case FUN -> "zap";
        };
    }

    private static int argb(final Color color) {
        return (color.getAlpha() & 0xFF) << 24
                | (color.getRed() & 0xFF) << 16
                | (color.getGreen() & 0xFF) << 8
                | (color.getBlue() & 0xFF);
    }

    private enum Tab {
        MODULES,
        FRIENDS,
        PROFILES,
        THEME
    }

    private enum Drawer {
        NONE,
        ROOT,
        GENERAL,
        MODULES,
        SOUND,
        NOTIFICATIONS
    }

    /**
     * Exponentially smoothed value for lightweight per-frame animation. The menu re-renders every
     * frame, so calling {@link #toward} inside {@code render} advances the animation without extra
     * layout invalidation.
     */
    private static final class Anim {

        private float value;
        private boolean initialized;
        private long lastNanos;

        float toward(final float target, final float speed) {
            long now = System.nanoTime();
            if (!this.initialized) {
                this.initialized = true;
                this.value = target;
                this.lastNanos = now;
                return this.value;
            }
            float deltaSeconds = Math.min(0.1F, (now - this.lastNanos) / 1_000_000_000F);
            this.lastNanos = now;
            this.value += (target - this.value) * (1F - (float) Math.exp(-speed * deltaSeconds));
            if (Math.abs(target - this.value) < 0.002F) {
                this.value = target;
            }
            return this.value;
        }
    }

    /**
     * A player's face (head + hat overlay) drawn from their skin. Resolves the skin asynchronously
     * through vanilla's skin cache, falling back to the default Steve/Alex skin until it loads.
     */
    private static final class FriendHead extends Component {

        private final ResolvableProfile profile;
        private final int size;

        private FriendHead(final String name, final int size) {
            this.profile = ResolvableProfile.createUnresolved(name);
            this.size = size;
            this.capabilities().all(false);
        }

        @Override
        public void render(final Renderer renderer, final Size bounds) {
            renderer.custom(new Blaze3DRenderCommand(
                    graphics -> {
                        try {
                            PlayerFaceExtractor.extractRenderState(graphics, this.profile, 0, 0, this.size);
                        } catch (RuntimeException ignored) {
                            // A skin that fails to resolve must never take down the render pass.
                        }
                    },
                    new Rectangle(0, 0, this.size, this.size)));
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(this.size, this.size);
        }
    }

    /**
     * A liquid glass panel background: refracts the blurred game scene captured by GlassBackdrop.
     * Fills its whole component bounds; layer it under content with a spanning grid cell.
     */
    private static final class GlassSurface extends Component {

        private final Supplier<Color> tint;
        private final Supplier<Float> cornerRadius;
        private final Supplier<BackgroundDesign> design;

        private GlassSurface(final Supplier<Color> tint, final Supplier<Float> cornerRadius,
                             final Supplier<BackgroundDesign> design) {
            this.tint = tint;
            this.cornerRadius = cornerRadius;
            this.design = design;
            this.capabilities().all(false);
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            renderer.custom(new GlassPanelCommand(0F, 0F, size.width(), size.height(),
                    this.cornerRadius.get(), this.tint.get(), this.design.get()));
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return Size.EMPTY;
        }
    }

    private GlassSurface glassPanel() {
        return new GlassSurface(GlassTheme::glass, GlassTheme::cornerRadius, () -> this.backgroundDesign);
    }

    private GlassSurface glassDeepPanel() {
        return new GlassSurface(GlassTheme::glassDeep, GlassTheme::cornerRadius, () -> this.backgroundDesign);
    }

    private static final class TextNode extends Component implements LayoutDebugLabel {

        private static final float LINE_SPACING = 2F;

        private final Supplier<String> text;
        private final Supplier<Color> color;
        private TextOrigin.Horizontal horizontal = TextOrigin.Horizontal.LOGICAL_LEFT;
        private TextOrigin.Vertical vertical = TextOrigin.Vertical.LOGICAL_CENTER;
        private int maxLines = 1;

        private TextNode(final Supplier<String> text, final Supplier<Color> color) {
            this.text = text;
            this.color = color;
            this.capabilities().all(false);
        }

        private TextNode origin(final TextOrigin.Horizontal horizontal, final TextOrigin.Vertical vertical) {
            this.horizontal = horizontal;
            this.vertical = vertical;
            return this;
        }

        /** Allow the text to fold onto extra lines (word wrap) instead of ellipsizing. */
        private TextNode maxLines(final int maxLines) {
            this.maxLines = Math.max(1, maxLines);
            return this;
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            String value = this.text.get();
            if (value.isBlank()) {
                return;
            }
            Color currentColor = this.color.get();
            float x = switch (this.horizontal) {
                case LOGICAL_LEFT, VISUAL_LEFT -> 0F;
                case VISUAL_CENTER -> size.width() / 2F;
                case VISUAL_RIGHT -> size.width();
            };
            if (this.maxLines <= 1) {
                drawFittedText(this, renderer, value, currentColor, x, size.height() / 2F,
                        size.width(), this.horizontal, this.vertical);
                return;
            }
            List<String> lines = wrapText(this, value, currentColor, size.width(), this.maxLines);
            float lineHeight = this.rivet().backend().font().height() + LINE_SPACING;
            float totalHeight = lines.size() * lineHeight - LINE_SPACING;
            float y = (size.height() - totalHeight) / 2F + (lineHeight - LINE_SPACING) / 2F;
            for (String line : lines) {
                drawText(this, renderer, line, currentColor, x, y, this.horizontal, this.vertical);
                y += lineHeight;
            }
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            if (this.rivet() == null) {
                return Size.EMPTY;
            }
            String value = this.text.get();
            if (value.isBlank()) {
                return Size.EMPTY;
            }
            float fontHeight = this.rivet().backend().font().height();
            if (this.maxLines <= 1) {
                // Zero ideal width: text always lives in weighted or fixed-width cells, and a long
                // string's measured width must never squeeze fixed siblings (toggles, icons).
                return new Size(0F, fontHeight);
            }
            int lines = Math.max(1, wrapText(this, value, this.color.get(), constraints.width(), this.maxLines).size());
            return new Size(0F, lines * (fontHeight + LINE_SPACING) - LINE_SPACING);
        }

        /** Number of wrapped lines at the given width; 1 while unattached or for single-line nodes. */
        private int lineCount(final float width) {
            if (this.maxLines <= 1 || this.rivet() == null || width <= 0F) {
                return 1;
            }
            String value = this.text.get();
            if (value.isBlank()) {
                return 1;
            }
            return Math.max(1, wrapText(this, value, this.color.get(), width, this.maxLines).size());
        }

        @Override
        public String layoutDebugLabel() {
            return this.text.get();
        }
    }

    private static final class IconNode extends Component implements LayoutDebugLabel {

        private final Supplier<String> icon;
        private final Supplier<Color> color;

        private IconNode(final Supplier<String> icon, final Supplier<Color> color) {
            this.icon = icon;
            this.color = color;
            this.fixedSize(ICON_BOX, ICON_BOX);
            this.capabilities().all(false);
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            float iconX = (size.width() - ICON_BOX) / 2F;
            float iconY = (size.height() - ICON_BOX) / 2F;
            renderer.translate(iconX, iconY, () -> drawIcon(renderer, this.icon.get(), ICON_BOX / 2F, ICON_BOX / 2F, this.color.get()));
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(ICON_BOX, ICON_BOX);
        }

        @Override
        public String layoutDebugLabel() {
            return this.icon.get();
        }
    }

    private static final class IconButton extends Component implements LayoutDebugLabel {

        private final Supplier<String> icon;
        private final Supplier<Color> color;
        private final Runnable action;
        private final BooleanSupplier visible;

        private IconButton(final Supplier<String> icon, final Supplier<Color> color, final Runnable action,
                           final BooleanSupplier visible) {
            this.icon = icon;
            this.color = color;
            this.action = action;
            this.visible = visible;
            this.fixedSize(ICON_BUTTON_SIZE, ICON_BUTTON_SIZE);
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            if (!this.visible.getAsBoolean()) {
                return;
            }
            drawIcon(renderer, this.icon.get(), size.width() / 2F, size.height() / 2F, this.color.get());
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
            if (!this.visible.getAsBoolean() || event.button() != MouseButton.LEFT) {
                return false;
            }
            this.action.run();
            return true;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(ICON_BUTTON_SIZE, ICON_BUTTON_SIZE);
        }

        @Override
        public String layoutDebugLabel() {
            return this.visible.getAsBoolean() ? this.icon.get() : "";
        }
    }

    private static final class TextAction extends Component implements LayoutDebugLabel {

        private final Supplier<String> text;
        private final Supplier<Color> color;
        private final Runnable action;

        private TextAction(final Supplier<String> text, final Supplier<Color> color, final Runnable action) {
            this.text = text;
            this.color = color;
            this.action = action;
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            String value = this.text.get();
            if (value.isBlank()) {
                return;
            }
            drawFittedText(this, renderer, value, this.color.get(), size.width(), size.height() / 2F,
                    size.width(), TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
            if (event.button() != MouseButton.LEFT) {
                return false;
            }
            this.action.run();
            return true;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            if (this.rivet() == null) {
                return Size.EMPTY;
            }
            String value = this.text.get();
            if (value.isBlank()) {
                return Size.EMPTY;
            }
            return new Size(Math.min(textWidth(this, value, this.color.get()), constraints.width()), this.rivet().backend().font().height());
        }

        @Override
        public String layoutDebugLabel() {
            return this.text.get();
        }
    }

    private static final class BrandBlock extends Component {

        private BrandBlock() {
            this.capabilities().all(false);
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            float clientWidth = textWidth(this, "client", GlassTheme.accent());
            float brandWidth = textWidth(this, "ANARCHY", TEXT);
            float x = size.width() - clientWidth - 6F - brandWidth;
            drawText(this, renderer, "ANARCHY", TEXT, x, size.height() / 2F,
                    TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            drawText(this, renderer, "client", GlassTheme.accent(), x + brandWidth + 6F, size.height() / 2F,
                    TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(148F, 16F);
        }
    }

    private static final class Surface extends Component {

        private final Supplier<Color> color;
        private Supplier<Color> outlineColor = () -> Color.TRANSPARENT;
        private float outlineWidth;
        private float cornerRadius;

        private Surface(final Supplier<Color> color) {
            this.color = color;
            this.capabilities().all(false);
        }

        private Surface outline(final Color color, final float width) {
            return this.outline(() -> color, width);
        }

        private Surface outline(final Supplier<Color> color, final float width) {
            this.outlineColor = color;
            this.outlineWidth = width;
            return this;
        }

        private Surface cornerRadius(final float cornerRadius) {
            this.cornerRadius = cornerRadius;
            return this;
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            Color fill = this.color.get();
            float radius = Math.min(this.cornerRadius, Math.min(size.width(), size.height()) / 2F);
            if (fill.getAlpha() > 0) {
                renderer.optimizedFillRoundedRect(0, 0, size.width(), size.height(), radius, fill);
            }
            Color outline = this.outlineColor.get();
            if (outline.getAlpha() > 0 && this.outlineWidth > 0F) {
                renderer.optimizedOutlineRoundedRect(0, 0, size.width(), size.height(), radius, this.outlineWidth, outline);
            }
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return Size.EMPTY;
        }
    }

    /**
     * Animated pill toggle. Owns its bounds and click handling so the painted switch and its
     * clickable area can never drift apart; the thumb glides between states.
     */
    private static final class ToggleSwitch extends Component {

        private final BooleanSupplier visible;
        private final BooleanSupplier state;
        private final Runnable onToggle;
        private final Anim thumb = new Anim();

        private ToggleSwitch(final BooleanSupplier state, final Runnable onToggle) {
            this(() -> true, state, onToggle);
        }

        private ToggleSwitch(final BooleanSupplier visible, final BooleanSupplier state, final Runnable onToggle) {
            this.visible = visible;
            this.state = state;
            this.onToggle = onToggle;
            this.fixedSize(SWITCH_WIDTH, SWITCH_HEIGHT);
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            if (!this.visible.getAsBoolean()) {
                return;
            }
            boolean checked = this.state.getAsBoolean();
            float progress = this.thumb.toward(checked ? 1F : 0F, 16F);
            float trackY = (SWITCH_HEIGHT - SWITCH_TRACK_HEIGHT) / 2F;
            float radius = SWITCH_TRACK_HEIGHT / 2F;
            Color track = Color.interpolate(progress, TRACK, GlassTheme.accent());
            renderer.optimizedFillRoundedRect(0, trackY, SWITCH_WIDTH, SWITCH_TRACK_HEIGHT, radius, track);
            float thumbX = radius + (SWITCH_WIDTH - 2F * radius) * progress;
            renderer.fillCircle(thumbX, SWITCH_HEIGHT / 2F, radius - 2F, Color.fromRGB(250, 250, 250));
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
            if (this.visible.getAsBoolean() && event.button() == MouseButton.LEFT) {
                this.onToggle.run();
                return true;
            }
            return false;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(SWITCH_WIDTH, SWITCH_HEIGHT);
        }
    }

    private final class Dock extends Container {

        private final GlassSurface background = new GlassSurface(GlassTheme::glass, () -> DOCK_WIDTH / 2F, () -> BackgroundDesign.NONE);
        private final List<Component> entries = new ArrayList<>();

        private Dock() {
            super(gridLayout(0, (int) DOCK_GAP));
            for (ModuleCategory category : ModuleCategory.values()) {
                this.entries.add(new DockButton(
                        categoryIcon(category),
                        category.displayName(),
                        () -> ModulePanel.this.selectedTab == Tab.MODULES && ModulePanel.this.selectedCategory == category,
                        () -> ModulePanel.this.selectCategory(category),
                        () -> ModulePanel.this.enabledCount(category) > 0
                ));
            }
            this.entries.add(new DockDivider());
            this.entries.add(new DockButton("palette", "Theme",
                    () -> ModulePanel.this.selectedTab == Tab.THEME,
                    () -> ModulePanel.this.selectTab(Tab.THEME),
                    () -> false));
            this.entries.add(new DockButton("users", "Friends",
                    () -> ModulePanel.this.selectedTab == Tab.FRIENDS,
                    () -> ModulePanel.this.selectTab(Tab.FRIENDS),
                    () -> false));
            this.entries.add(new DockButton("layers", "Profiles",
                    () -> ModulePanel.this.selectedTab == Tab.PROFILES,
                    () -> ModulePanel.this.selectTab(Tab.PROFILES),
                    () -> false));
            this.entries.add(new DockDivider());
            this.entries.add(new DockButton("settings", "Settings",
                    () -> ModulePanel.this.drawer != Drawer.NONE,
                    // openDrawer toggles: clicking the gear again closes the drawer.
                    () -> ModulePanel.this.openDrawer(Drawer.ROOT),
                    () -> false));

            place(this.background, fillCell(0, 0, 1, this.entries.size()));
            this.addChild(this.background);
            int row = 0;
            for (Component entry : this.entries) {
                float height = entry instanceof DockDivider ? DOCK_DIVIDER : DOCK_BUTTON;
                float topPad = row == 0 ? 8F : 0F;
                float bottomPad = row == this.entries.size() - 1 ? 8F : 0F;
                place(entry, cell(0, row, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                        padding(6F, topPad, 6F, bottomPad), null, height));
                this.addChild(entry);
                row++;
            }
        }

        private float dockHeight() {
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

    private static final class DockDivider extends Component {

        private DockDivider() {
            this.capabilities().all(false);
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            float width = size.width() - 10F;
            renderer.fillRect(5F, size.height() / 2F, width, 1F, DIVIDER);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), DOCK_DIVIDER);
        }
    }

    private final class DockButton extends Component implements LayoutDebugLabel {

        private final String icon;
        private final String name;
        private final BooleanSupplier selected;
        private final Runnable action;
        private final BooleanSupplier activeDot;
        private final Anim hover = new Anim();
        private boolean hovered;

        private DockButton(final String icon, final String name, final BooleanSupplier selected,
                           final Runnable action, final BooleanSupplier activeDot) {
            this.icon = icon;
            this.name = name;
            this.selected = selected;
            this.action = action;
            this.activeDot = activeDot;
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            boolean isSelected = this.selected.getAsBoolean();
            float glow = this.hover.toward(this.hovered || isSelected ? 1F : 0F, 14F);
            if (glow > 0.01F) {
                int alpha = Math.round((isSelected ? 40F : 26F) * glow);
                renderer.optimizedFillRoundedRect(1F, 1F, size.width() - 2F, size.height() - 2F, 9F,
                        Color.fromRGBA(255, 255, 255, alpha));
            }
            Color iconColor = isSelected ? GlassTheme.accent() : (this.hovered ? TEXT : MUTED);
            drawIcon(renderer, this.icon, size.width() / 2F, size.height() / 2F, iconColor);
            if (this.activeDot.getAsBoolean()) {
                renderer.fillCircle(size.width() - 6F, 6F, 2F, GlassTheme.accent());
            }
        }

        @Override
        protected void onComponentMouseEnter() {
            this.hovered = true;
        }

        @Override
        protected void onComponentMouseLeave() {
            this.hovered = false;
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
            if (event.button() == MouseButton.LEFT) {
                this.action.run();
                return true;
            }
            return false;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), DOCK_BUTTON);
        }

        @Override
        public String layoutDebugLabel() {
            return this.name.toLowerCase(Locale.ROOT);
        }
    }

    private final class ModulesPanel extends Container implements LayoutDebugLabel {

        private final GlassSurface background = ModulePanel.this.glassPanel();
        private final TextNode title = textNode(this::title, () -> TEXT);
        private final TextNode count = textNode(this::countText, GlassTheme::accent)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        private final SearchInput search = new SearchInput("Search all modules...");
        // HUD editor entry point: shown top-right only on the HUD tab. Positioning moved out of the
        // per-module settings into a drag editor, so this button is how you reach it.
        private final IconButton editHud = iconButton("move", GlassTheme::accent,
                ModulePanel.this::openHudEditor, this::showEditHud);
        private final CardGrid cards = new CardGrid();
        private final ScrollContainer scroll = new LayoutDebugScrollContainer(this.cards);

        private ModulesPanel() {
            super(gridLayout(0, 0));
            ModulePanel.this.configureScroll(this.scroll);
            this.search.onChange(value -> {
                ModulePanel.this.searchQuery = value.trim();
                ModulePanel.this.refreshModuleList();
            });
            place(this.background, fillCell(0, 0, 2, 3));
            place(this.title, cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING + 2F, 6F, 8F, 0F), null, HEADER_HEIGHT));
            place(this.count, cell(1, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 6F, PADDING + 2F, 0F), 52F, HEADER_HEIGHT));
            place(this.editHud, cell(1, 0, 1, 1, 0F, 0F, GridAnchor.RIGHT, GridFill.NONE,
                    padding(0F, 6F, PADDING, 0F), ICON_BUTTON_SIZE, ICON_BUTTON_SIZE));
            place(this.search, cell(0, 1, 2, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, PADDING, 6F), null, SEARCH_HEIGHT));
            place(this.scroll, cell(0, 2, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 4F, PADDING, PADDING), null, null));
            this.addChild(this.background);
            this.addChild(this.title);
            this.addChild(this.count);
            this.addChild(this.editHud);
            this.addChild(this.search);
            this.addChild(this.scroll);
        }

        private boolean showEditHud() {
            return ModulePanel.this.selectedTab == Tab.MODULES
                    && ModulePanel.this.selectedCategory == ModuleCategory.HUD
                    && ModulePanel.this.searchQuery.isEmpty();
        }

        private void refresh() {
            this.cards.clearChildren();
            List<Module> visible = ModulePanel.this.visibleModules();
            if (visible.isEmpty()) {
                this.cards.addChild(new EmptyState("No modules match the current filter."));
            } else {
                for (Module module : visible) {
                    this.cards.addChild(new ModuleCard(module));
                }
            }
        }

        private String title() {
            if (!ModulePanel.this.searchQuery.isEmpty()) {
                return "Search";
            }
            return ModulePanel.this.selectedCategory == null ? "Modules" : ModulePanel.this.selectedCategory.displayName();
        }

        private String countText() {
            // The HUD tab shows the editor button here instead of a count.
            if (this.showEditHud() || !ModulePanel.this.searchQuery.isEmpty() || ModulePanel.this.selectedCategory == null) {
                return "";
            }
            int count = ModulePanel.this.enabledCount(ModulePanel.this.selectedCategory);
            return count > 0 ? count + " on" : "";
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return constraints;
        }

        @Override
        public String layoutDebugLabel() {
            return this.title();
        }
    }

    /**
     * Two-column card grid. Rivet's list layouts stack a single column, so this container places
     * cards itself and reports the resulting content height for the surrounding scroll container.
     */
    private final class CardGrid extends Container {

        private CardGrid() {
            super(AbsoluteLayout.INSTANCE);
        }

        private float cardHeight() {
            return ModulePanel.this.compactRows ? 40F : 46F;
        }

        @Override
        public void computeLayout(final Size size) {
            float cardWidth = (size.width() - CARD_GAP) / 2F;
            float height = this.cardHeight();
            int index = 0;
            for (Component child : this.children()) {
                if (child instanceof EmptyState) {
                    child.layoutOptions(new AbsoluteOptions(0F, 0F, size.width(), 64F));
                    continue;
                }
                int column = index % 2;
                int row = index / 2;
                child.layoutOptions(new AbsoluteOptions(
                        column * (cardWidth + CARD_GAP),
                        row * (height + CARD_GAP),
                        cardWidth,
                        height
                ));
                index++;
            }
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            int cards = 0;
            boolean empty = false;
            for (Component child : this.children()) {
                if (child instanceof EmptyState) {
                    empty = true;
                } else {
                    cards++;
                }
            }
            if (empty && cards == 0) {
                return new Size(constraints.width(), 64F);
            }
            int rows = (cards + 1) / 2;
            float height = rows == 0 ? 0F : rows * this.cardHeight() + (rows - 1) * CARD_GAP;
            return new Size(constraints.width(), height);
        }
    }

    private final class ModuleCard extends Container implements LayoutDebugLabel {

        private final Module module;
        private final Surface background;
        private final Surface hairline;
        private final IconNode icon;
        private final TextNode name;
        private final IconButton settingsButton;
        private final Anim hover = new Anim();
        private boolean hovered;

        private ModuleCard(final Module module) {
            super(gridLayout(0, 0));
            this.module = module;
            this.background = new Surface(this::backgroundColor).cornerRadius(10F);
            this.hairline = new Surface(() -> this.module.enabled() ? GlassTheme.accent() : Color.TRANSPARENT).cornerRadius(1F);
            this.icon = iconNode(module::icon, () -> this.module.enabled() ? GlassTheme.accent() : MUTED);
            this.name = textNode(module.name(), () -> this.module.enabled() ? TEXT : MUTED);
            this.settingsButton = iconButton("sliders", () -> this.inspected() ? GlassTheme.accent() : FAINT,
                    this::toggleInspect);
            place(this.background, fillCell(0, 0, 4, 1));
            place(this.hairline, cell(0, 0, 4, 1, 0F, 0F, GridAnchor.TOP, GridFill.HORIZONTAL,
                    padding(10F, 0F, 10F, 0F), null, 2F));
            place(this.icon, cell(0, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.NONE,
                    padding(7F, 0F, 0F, 0F), ICON_BOX, ICON_BOX));
            place(this.name, cell(1, 0, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(7F, 0F, 4F, 0F), null, null));
            place(this.settingsButton, cell(3, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 2F, 0F), 24F, 24F));
            this.addChild(this.background);
            this.addChild(this.hairline);
            this.addChild(this.icon);
            this.addChild(this.name);
            this.addChild(this.settingsButton);
        }

        private void toggleInspect() {
            if (this.inspected()) {
                ModulePanel.this.closeInspector();
            } else {
                ModulePanel.this.inspectModule(this.module);
            }
        }

        @Override
        protected boolean onComponentMouseMove(final MouseMoveEvent event, final Size size) {
            // Rivet only marks a component as hovered if it consumes mouse moves. A container
            // consumes only where an interactive child sits, which limited the card's hover
            // (highlight + tooltip) to the settings icon. The whole card is a hover target.
            super.onComponentMouseMove(event, size);
            return true;
        }

        @Override
        protected void onComponentMouseEnter() {
            this.hovered = true;
            ModulePanel.this.hoveredModule = this.module;
        }

        @Override
        protected void onComponentMouseLeave() {
            this.hovered = false;
            if (ModulePanel.this.hoveredModule == this.module) {
                ModulePanel.this.hoveredModule = null;
            }
            super.onComponentMouseLeave();
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
            if (super.onComponentMouseDown(event, size)) {
                return true;
            }
            if (event.button() == MouseButton.LEFT) {
                ModulePanel.this.toggleModule(this.module);
                return true;
            }
            if (event.button() == MouseButton.RIGHT) {
                // Right-click opens the module's settings; right-clicking again closes them.
                this.toggleInspect();
                return true;
            }
            return false;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), ModulePanel.this.modulesPanel.cards.cardHeight());
        }

        @Override
        public String layoutDebugLabel() {
            return this.module.id();
        }

        private boolean inspected() {
            return this.module == ModulePanel.this.inspectedModule;
        }

        private Color backgroundColor() {
            float glow = this.hover.toward(this.hovered ? 1F : 0F, 14F);
            Color base = this.module.enabled() ? GlassTheme.accentSoft().multiplyAlpha(0.55F) : CARD;
            Color hoverColor = this.module.enabled() ? GlassTheme.accentSoft() : CARD_HOVER;
            return Color.interpolate(glow, base, hoverColor);
        }
    }

    private final class SearchInput extends Container implements LayoutDebugLabel {

        private static final float FIELD_TEXT_INSET = 6F;

        private final String placeholder;
        private final Surface background = surface(FIELD)
                .outline(() -> this.fieldFocused() ? GlassTheme.accent() : Color.TRANSPARENT, 1F)
                .cornerRadius(9F);
        private final IconNode icon = iconNode("search", () -> this.fieldFocused() ? MUTED : FAINT);
        private final TextNode placeholderLabel;
        private final TextField field = textField("");
        private Consumer<String> changeListener = ignored -> {
        };

        private SearchInput(final String placeholder) {
            super(gridLayout(0, 0));
            this.placeholder = placeholder;
            this.placeholderLabel = textNode(() -> this.field.text().isEmpty() && !this.fieldFocused() ? this.placeholder : "", () -> FAINT);
            this.field.backgroundColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.outlineColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.focusedOutlineColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.innerPadding().set(new Padding(FIELD_TEXT_INSET, 0, FIELD_TEXT_INSET, 0));
            this.field.valueChangeListener().add(value -> this.changeListener.accept(value));
            place(this.background, fillCell(0, 0, 2, 1));
            place(this.icon, cell(0, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.NONE,
                    padding(8F, 0F, 0F, 0F), ICON_BOX, ICON_BOX));
            // The placeholder's left inset matches the field's inner text inset so the caret and
            // the placeholder text sit in exactly the same spot.
            place(this.placeholderLabel, cell(1, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.HORIZONTAL,
                    padding(FIELD_TEXT_INSET, 0F, 8F, 0F), null, 18F));
            place(this.field, cell(1, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.HORIZONTAL,
                    padding(0F, 0F, 4F, 0F), null, 18F));
            this.addChild(this.background);
            this.addChild(this.icon);
            this.addChild(this.placeholderLabel);
            this.addChild(this.field);
        }

        private void onChange(final Consumer<String> listener) {
            this.changeListener = listener;
        }

        private void text(final String value) {
            this.field.text(value);
            this.changeListener.accept(value);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), SEARCH_HEIGHT);
        }

        @Override
        public String layoutDebugLabel() {
            return this.field.text().isBlank() ? "empty" : this.field.text();
        }

        private boolean fieldFocused() {
            return this.field.rivet() != null && this.field.rivet().focusedComponent() == this.field;
        }
    }

    private final class ModuleInspector extends Container implements LayoutDebugLabel {

        private final GlassSurface background = ModulePanel.this.glassDeepPanel();
        private final InspectorHeader header = new InspectorHeader();
        private final Container settings = new Container(new VerticalListLayout(0, true));
        private final ScrollContainer scroll = new LayoutDebugScrollContainer(this.settings);

        private ModuleInspector() {
            super(gridLayout(0, 0));
            ModulePanel.this.configureScroll(this.scroll);
            place(this.background, fillCell(0, 0, 1, 2));
            // No fixed height: the header grows when the module description folds onto a second line.
            place(this.header, cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    Padding.EMPTY, null, null));
            place(this.scroll, cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 0F, PADDING), null, null));
            this.addChild(this.background);
            this.addChild(this.header);
            this.addChild(this.scroll);
        }

        private void refresh() {
            this.settings.clearChildren();
            Module module = ModulePanel.this.inspectedModule;
            if (module == null) {
                this.settings.addChild(new EmptyState("Open a module to edit its settings."));
            } else {
                this.settings.addChild(new KeybindRow(module));
                List<SettingGroup> groups = visibleSettingGroups(module);
                boolean groupHeaders = showGroupHeaders(groups);
                for (SettingGroup group : groups) {
                    if (groupHeaders) {
                        this.settings.addChild(new GroupHeader(group.name()));
                    }
                    for (Setting<?> setting : group.settings()) {
                        if (setting.visible()) {
                            this.settings.addChild(settingRow(module, setting, this::refreshAfterSettingChange, ModulePanel.this.config::save));
                        }
                    }
                }
                if (this.settings.children().isEmpty()) {
                    this.settings.addChild(new EmptyState("This module has no visible settings."));
                }
            }
            ModulePanel.this.requestFrame();
        }

        private void refreshAfterSettingChange() {
            ModulePanel.this.config.save();
            this.refresh();
            ModulePanel.this.refreshModuleList();
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return constraints;
        }

        @Override
        public String layoutDebugLabel() {
            return ModulePanel.this.inspectedModule == null ? "empty" : ModulePanel.this.inspectedModule.id();
        }
    }

    private final class InspectorHeader extends Container implements LayoutDebugLabel {

        private final Surface divider = surface(DIVIDER);
        private final IconNode moduleIcon = iconNode(this::moduleIcon, this::moduleIconColor);
        private final TextNode title = textNode(() -> ModulePanel.this.inspectedModule == null ? "No module" : ModulePanel.this.inspectedModule.name(),
                () -> ModulePanel.this.inspectedModule == null ? MUTED : TEXT);
        private final TextNode category = textNode(
                () -> ModulePanel.this.inspectedModule == null ? "" : ModulePanel.this.inspectedModule.category().displayName(),
                () -> FAINT
        );
        private final TextNode description = textNode(
                () -> ModulePanel.this.inspectedModule == null ? "" : ModulePanel.this.inspectedModule.description(),
                () -> FAINT
        ).maxLines(Integer.MAX_VALUE);
        private final IconButton star;
        private final ToggleSwitch toggle;
        private final IconButton close;

        private InspectorHeader() {
            super(gridLayout(0, 0));
            this.star = iconButton("star", this::starColor, () -> {
                if (ModulePanel.this.inspectedModule != null) {
                    ModulePanel.this.toggleFavorite(ModulePanel.this.inspectedModule);
                }
            });
            this.toggle = new ToggleSwitch(
                    () -> ModulePanel.this.inspectedModule != null,
                    () -> ModulePanel.this.inspectedModule != null && ModulePanel.this.inspectedModule.enabled(),
                    () -> {
                        if (ModulePanel.this.inspectedModule != null) {
                            ModulePanel.this.toggleModule(ModulePanel.this.inspectedModule);
                        }
                    });
            this.close = iconButton("x", () -> FAINT, ModulePanel.this::closeInspector);
            place(this.divider, cell(0, 3, 5, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    padding(PADDING, 0F, PADDING, 0F), null, 1F));
            place(this.moduleIcon, cell(0, 0, 1, 3, 0F, 0F, GridAnchor.CENTER, GridFill.NONE,
                    padding(PADDING, 0F, 0F, 0F), ICON_BOX, ICON_BOX));
            place(this.title, cell(1, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(8F, 10F, 8F, 0F), null, 20F));
            place(this.category, cell(1, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(8F, 0F, 8F, 0F), null, 16F));
            place(this.description, cell(1, 2, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(8F, 0F, 8F, 8F), null, null));
            place(this.star, fixedCell(2, 0, 1, 2, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE, GridAnchor.CENTER));
            place(this.toggle, cell(3, 0, 1, 2, 0F, 0F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 4F, 0F), SWITCH_WIDTH, SWITCH_HEIGHT));
            place(this.close, cell(4, 0, 1, 2, 0F, 0F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 6F, 0F), ICON_BUTTON_SIZE, ICON_BUTTON_SIZE));
            this.addChild(this.divider);
            this.addChild(this.moduleIcon);
            this.addChild(this.title);
            this.addChild(this.category);
            this.addChild(this.description);
            this.addChild(this.star);
            this.addChild(this.toggle);
            this.addChild(this.close);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            float descriptionWidth = Math.max(0F, constraints.width() - PADDING - ICON_BOX - 8F - 16F);
            float extra = (this.description.lineCount(descriptionWidth) - 1) * 11F;
            return new Size(constraints.width(), INSPECTOR_HEADER_HEIGHT + extra);
        }

        @Override
        public String layoutDebugLabel() {
            return ModulePanel.this.inspectedModule == null ? "empty" : ModulePanel.this.inspectedModule.id();
        }

        private String moduleIcon() {
            Module module = ModulePanel.this.inspectedModule;
            return module == null ? "" : module.icon();
        }

        private Color moduleIconColor() {
            Module module = ModulePanel.this.inspectedModule;
            if (module == null) {
                return Color.TRANSPARENT;
            }
            return module.enabled() ? GlassTheme.accent() : MUTED;
        }

        private Color starColor() {
            Module module = ModulePanel.this.inspectedModule;
            if (module == null) {
                return Color.TRANSPARENT;
            }
            return ModulePanel.this.config.moduleFavorite(module.id()) ? WARNING : FAINT;
        }
    }

    private static final class GroupHeader extends Container implements LayoutDebugLabel {

        private final String name;
        private final TextNode label;

        private GroupHeader(final String name) {
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

    private abstract static class SettingLine extends Container implements LayoutDebugLabel {

        private static final float WRAP_LINE_HEIGHT = 11F;

        private final Setting<?> setting;
        private final float height;
        private final Surface divider = surface(DIVIDER);
        private final TextNode label;
        private final TextNode description;

        private SettingLine(final Module module, final Setting<?> setting, final float height) {
            super(gridLayout(0, 0));
            this.setting = setting;
            this.height = height;
            this.label = textNode(setting.name(), MUTED);
            this.description = textNode(() -> module.settingDescription(setting), () -> FAINT).maxLines(Integer.MAX_VALUE);
            this.addChild(this.divider);
            this.addChild(this.label);
            this.addChild(this.description);
        }

        @Override
        public void computeLayout(final Size size) {
            float extra = this.extraHeight(size.width());
            place(this.divider, cell(0, 2, 4, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    padding(PADDING, 0F, PADDING, 0F), null, 1F));
            place(this.label, cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 4F, 8F, 0F), null, 18F));
            place(this.description, cell(0, 1, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, 8F, 5F), null, 17F + extra));
            this.layoutControls(size);
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), this.height + this.extraHeight(constraints.width()));
        }

        /** Row grows when the description folds onto a second line instead of ellipsizing. */
        private float extraHeight(final float rowWidth) {
            float available = Math.max(0F, rowWidth - PADDING - 8F - this.controlReserve(rowWidth));
            return (this.description.lineCount(available) - 1) * WRAP_LINE_HEIGHT;
        }

        /** Horizontal space claimed by this row's control columns, kept clear of the description. */
        protected float controlReserve(final float rowWidth) {
            return SWITCH_WIDTH + 14F;
        }

        protected void layoutControls(final Size size) {
        }

        @Override
        public String layoutDebugLabel() {
            return this.setting.id();
        }
    }

    private static final class BooleanSettingRow extends SettingLine {

        private final BooleanSetting setting;
        private final Runnable onChange;
        private final ToggleSwitch toggle;

        private BooleanSettingRow(final Module module, final BooleanSetting setting, final Runnable onChange) {
            super(module, setting, SETTING_ROW_HEIGHT);
            this.setting = setting;
            this.onChange = onChange;
            this.toggle = new ToggleSwitch(this.setting::value, this::toggle);
            this.addChild(this.toggle);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
            if (super.onComponentMouseDown(event, size)) {
                return true;
            }
            if (event.button() == MouseButton.LEFT) {
                this.toggle();
                return true;
            }
            return false;
        }

        @Override
        protected void layoutControls(final Size size) {
            place(this.toggle, cell(3, 0, 1, 2, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 14F, 0F), SWITCH_WIDTH, SWITCH_HEIGHT));
        }

        private void toggle() {
            this.setting.value(!this.setting.value());
            this.onChange.run();
        }
    }

    private static final class SelectSettingRow extends SettingLine {

        private final SelectSetting setting;
        private final Runnable onChange;
        private final TextNode value;
        private final IconNode icon = iconNode("chevron-down", FAINT);

        private SelectSettingRow(final Module module, final SelectSetting setting, final Runnable onChange) {
            super(module, setting, SETTING_ROW_HEIGHT);
            this.setting = setting;
            this.onChange = onChange;
            this.value = textNode(this.setting::value, () -> TEXT)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.addChild(this.value);
            this.addChild(this.icon);
        }

        @Override
        protected float controlReserve(final float rowWidth) {
            return Math.min(100F, Math.max(0F, rowWidth * 0.42F)) + ICON_BOX + 10F;
        }

        @Override
        protected void layoutControls(final Size size) {
            float valueWidth = Math.min(100F, Math.max(0F, size.width() * 0.42F));
            place(this.value, cell(2, 0, 1, 2, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 2F, 0F), valueWidth, null));
            place(this.icon, cell(3, 0, 1, 2, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 8F, 0F), ICON_BOX, ICON_BOX));
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
            if (event.button() == MouseButton.LEFT) {
                this.setting.next();
                this.onChange.run();
                return true;
            }
            return false;
        }
    }

    private static final class ValueSettingRow extends SettingLine {

        private final Setting<?> setting;
        private final TextNode value;

        private ValueSettingRow(final Module module, final Setting<?> setting) {
            super(module, setting, SETTING_ROW_HEIGHT);
            this.setting = setting;
            this.value = textNode(() -> SettingControls.displayValue(this.setting), () -> TEXT)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.addChild(this.value);
        }

        @Override
        protected float controlReserve(final float rowWidth) {
            return Math.min(112F, Math.max(0F, rowWidth * 0.44F)) + PADDING;
        }

        @Override
        protected void layoutControls(final Size size) {
            float valueWidth = Math.min(112F, Math.max(0F, size.width() * 0.44F));
            place(this.value, cell(2, 0, 2, 2, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, PADDING, 0F), valueWidth, null));
        }
    }

    /**
     * Module-level keybind control: shows the bound key on the right, click to listen for the next
     * key press. Actual capture happens in the screen (see {@link ModulePanel#handleKeybindCapture}).
     */
    private final class KeybindRow extends Container implements LayoutDebugLabel {

        private final Module module;
        private final Surface divider = surface(DIVIDER);
        private final TextNode label = textNode("Keybind", MUTED);
        private final TextNode description;
        private final TextNode value;

        private KeybindRow(final Module module) {
            super(gridLayout(0, 0));
            this.module = module;
            this.description = textNode(this::descriptionText, () -> FAINT);
            this.value = textNode(this::valueText, this::valueColor)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.addChild(this.divider);
            this.addChild(this.label);
            this.addChild(this.description);
            this.addChild(this.value);
        }

        @Override
        public void computeLayout(final Size size) {
            float valueWidth = Math.min(120F, Math.max(0F, size.width() * 0.45F));
            place(this.divider, cell(0, 2, 4, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    padding(PADDING, 0F, PADDING, 0F), null, 1F));
            place(this.label, cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 4F, 8F, 0F), null, 18F));
            place(this.description, cell(0, 1, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, 8F, 5F), null, 17F));
            place(this.value, cell(2, 0, 2, 2, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, PADDING, 0F), valueWidth, null));
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), SETTING_ROW_HEIGHT);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
            if (event.button() == MouseButton.LEFT) {
                ModulePanel.this.startKeybindListening(this.module);
                return true;
            }
            return false;
        }

        private String valueText() {
            return ModulePanel.this.isListeningFor(this.module) ? "Press a key..." : keyLabel(this.module.keybind().key());
        }

        private Color valueColor() {
            if (ModulePanel.this.isListeningFor(this.module)) {
                return GlassTheme.accent();
            }
            return this.module.keybind().bound() ? TEXT : FAINT;
        }

        private String descriptionText() {
            return ModulePanel.this.isListeningFor(this.module) ? "Press a key, Esc to clear" : "Click to bind a key";
        }

        @Override
        public String layoutDebugLabel() {
            return "keybind";
        }
    }

    private static final class NumberSettingRow extends Container implements LayoutDebugLabel {

        private final NumberSetting setting;
        private final Surface divider = surface(DIVIDER);
        private final TextNode label;
        private final TextNode description;
        private final TextNode value;
        private final Slider slider;
        private final Runnable save;

        private NumberSettingRow(final Module module, final NumberSetting setting, final Runnable save) {
            super(gridLayout(0, 0));
            this.setting = setting;
            this.save = save;
            this.label = textNode(setting.name(), MUTED);
            this.description = textNode(() -> module.settingDescription(setting), () -> FAINT).maxLines(Integer.MAX_VALUE);
            this.value = textNode(() -> SettingControls.displayValue(this.setting), () -> TEXT)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.slider = slider(setting.min(), setting.max(), setting.step(), setting.value());
            this.slider.valueChangeListener().add(value -> {
                setting.value(value);
                this.save.run();
            });
            this.addChild(this.divider);
            this.addChild(this.label);
            this.addChild(this.description);
            this.addChild(this.value);
            this.addChild(this.slider);
        }

        @Override
        public void computeLayout(final Size size) {
            float valueWidth = Math.min(58F, Math.max(0F, size.width() * 0.25F));
            float extra = this.extraHeight(size.width());
            place(this.divider, cell(0, 3, 2, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    padding(PADDING, 0F, PADDING, 0F), null, 1F));
            place(this.label, cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 4F, 8F, 0F), null, 18F));
            place(this.value, cell(1, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 4F, PADDING, 0F), valueWidth, 18F));
            place(this.description, cell(0, 1, 2, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, PADDING, 0F), null, 17F + extra));
            place(this.slider, cell(0, 2, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.HORIZONTAL,
                    padding(PADDING, 0F, PADDING, 0F), null, 16F));
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), NUMBER_ROW_HEIGHT + this.extraHeight(constraints.width()));
        }

        private float extraHeight(final float rowWidth) {
            return (this.description.lineCount(Math.max(0F, rowWidth - PADDING * 2F)) - 1) * 11F;
        }

        @Override
        public String layoutDebugLabel() {
            return this.setting.id();
        }
    }

    private static final class TextSettingRow extends Container implements LayoutDebugLabel {

        private final Setting<?> setting;
        private final TextValueSetting textSetting;
        private final Surface divider = surface(DIVIDER);
        private final TextNode label;
        private final TextNode description;
        private final TextField field;
        private final Runnable save;

        private TextSettingRow(final Module module, final Setting<?> setting, final TextValueSetting textSetting, final Runnable save) {
            super(gridLayout(0, 0));
            this.setting = setting;
            this.textSetting = textSetting;
            this.save = save;
            this.label = textNode(setting.name(), MUTED);
            this.description = textNode(() -> module.settingDescription(setting), () -> FAINT).maxLines(Integer.MAX_VALUE);
            this.field = textField(textSetting.valueString());
            this.field.valueChangeListener().add(value -> {
                try {
                    this.textSetting.valueFromString(value);
                    this.save.run();
                } catch (IllegalArgumentException ignored) {
                    // Keep the last valid value while an incomplete structured value is being typed.
                }
            });
            this.addChild(this.divider);
            this.addChild(this.label);
            this.addChild(this.description);
            this.addChild(this.field);
        }

        @Override
        public void computeLayout(final Size size) {
            float extra = this.extraHeight(size.width());
            place(this.divider, cell(0, 2, 2, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    padding(PADDING, 0F, PADDING, 0F), null, 1F));
            place(this.label, cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 4F, 8F, 0F), null, 18F));
            place(this.description, cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, 8F, 5F), null, 17F + extra));
            place(this.field, cell(1, 0, 1, 2, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 7F, 10F, 7F), null, 24F));
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), TEXT_ROW_HEIGHT + this.extraHeight(constraints.width()));
        }

        private float extraHeight(final float rowWidth) {
            // The text field takes roughly the right half of the row.
            return (this.description.lineCount(Math.max(0F, rowWidth * 0.5F - PADDING - 8F)) - 1) * 11F;
        }

        @Override
        public String layoutDebugLabel() {
            return this.setting.id();
        }
    }

    private final class FriendsPanel extends Container implements LayoutDebugLabel {

        private final GlassSurface background = ModulePanel.this.glassPanel();
        private final TextNode title = textNode("Friends", TEXT);
        private final TextField addField = textField("");
        private final Button addButton = button("Add", TEXT, ignored -> this.addFriend());
        private final Container rows = new Container(new VerticalListLayout(4, true));
        private final ScrollContainer scroll = new LayoutDebugScrollContainer(this.rows);

        private FriendsPanel() {
            super(gridLayout(8, 4));
            ModulePanel.this.configureScroll(this.scroll);
            onEnter(this.addField, this::addFriend);
            place(this.background, fillCell(0, 0, 2, 3));
            place(this.title, cell(0, 0, 2, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING + 2F, 8F, PADDING, 0F), null, 26F));
            place(this.addField, cell(0, 1, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, 0F, 0F), null, null));
            place(this.addButton, cell(1, 1, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, PADDING, 0F), 62F, 28F));
            place(this.scroll, cell(0, 2, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 4F, PADDING, PADDING), null, null));
            this.addChild(this.background);
            this.addChild(this.title);
            this.addChild(this.addField);
            this.addChild(this.addButton);
            this.addChild(this.scroll);
        }

        private void refresh() {
            this.rows.clearChildren();
            Collection<String> friends = AnarchyClient.FRIENDS.friends();
            if (friends.isEmpty()) {
                this.rows.addChild(new EmptyState("No friends saved."));
            } else {
                for (String friend : friends) {
                    this.rows.addChild(new FriendRow(friend));
                }
            }
            ModulePanel.this.requestFrame();
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

    private final class FriendRow extends Container implements LayoutDebugLabel {

        private static final int HEAD_SIZE = 20;

        private final String friend;
        private final Surface background = new Surface(() -> CARD).cornerRadius(10F);
        private final FriendHead head;
        private final TextNode name;
        private final TextAction remove;

        private FriendRow(final String friend) {
            super(gridLayout(0, 0));
            this.friend = friend;
            this.fixedSize(-1, 34);
            this.head = new FriendHead(friend, HEAD_SIZE);
            this.name = textNode(friend, TEXT);
            this.remove = textAction("Remove", MUTED, () -> {
                if (AnarchyClient.FRIENDS.remove(this.friend)) {
                    ModulePanel.this.friendsPanel.refresh();
                }
            });
            place(this.background, fillCell(0, 0, 3, 1));
            place(this.head, cell(0, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                    padding(PADDING, 0F, 0F, 0F), (float) HEAD_SIZE, (float) HEAD_SIZE));
            place(this.name, cell(1, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(10F, 0F, 8F, 0F), null, null));
            place(this.remove, cell(2, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, PADDING, 0F), 76F, null));
            this.addChild(this.background);
            this.addChild(this.head);
            this.addChild(this.name);
            this.addChild(this.remove);
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

    private final class ProfilesPanel extends Container implements LayoutDebugLabel {

        private final GlassSurface background = ModulePanel.this.glassPanel();
        private final TextNode title = textNode("Profiles", TEXT);
        private final TextField nameField = textField("");
        private final Button saveButton = button("Capture", TEXT, ignored -> this.capture());
        private final Container rows = new Container(new VerticalListLayout(4, true));
        private final ScrollContainer scroll = new LayoutDebugScrollContainer(this.rows);

        private ProfilesPanel() {
            super(gridLayout(8, 4));
            ModulePanel.this.configureScroll(this.scroll);
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

        private void refresh() {
            this.rows.clearChildren();
            List<ProfileManager.ProfileSummary> profiles = AnarchyClient.PROFILES.summaries();
            if (profiles.isEmpty()) {
                this.rows.addChild(new EmptyState("No profiles captured."));
            } else {
                for (ProfileManager.ProfileSummary profile : profiles) {
                    this.rows.addChild(new ProfileRow(profile));
                }
            }
            ModulePanel.this.requestFrame();
        }

        private void capture() {
            String name = this.nameField.text().trim();
            if (name.isEmpty()) {
                return;
            }
            AnarchyClient.PROFILES.capture(name, ModulePanel.this.modules);
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

    private final class ProfileRow extends Container implements LayoutDebugLabel {

        private final ProfileManager.ProfileSummary profile;
        private final Surface background = new Surface(() -> CARD).cornerRadius(10F);
        private final TextNode name;
        private final TextNode modules;
        private final TextAction apply;
        private final TextAction delete;

        private ProfileRow(final ProfileManager.ProfileSummary profile) {
            super(gridLayout(0, 0));
            this.profile = profile;
            this.fixedSize(-1, 44);
            this.name = textNode(profile.name(), TEXT);
            this.modules = textNode(profile.modules() + " modules", FAINT);
            this.apply = textAction("Apply", GlassTheme.accent(), () -> {
                AnarchyClient.PROFILES.apply(this.profile.name(), ModulePanel.this.modules);
                ModulePanel.this.config.save();
                ModulePanel.this.refreshModuleList();
                ModulePanel.this.inspector.refresh();
            });
            this.delete = textAction("Delete", MUTED, () -> {
                if (AnarchyClient.PROFILES.delete(this.profile.name())) {
                    ModulePanel.this.profilesPanel.refresh();
                }
            });
            place(this.background, fillCell(0, 0, 3, 2));
            place(this.name, cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, 8F, 0F), null, 18F));
            place(this.modules, cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, 8F, 0F), null, 18F));
            place(this.apply, cell(1, 0, 1, 2, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 10F, 0F), 52F, null));
            place(this.delete, cell(2, 0, 1, 2, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
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

    /**
     * The Theme tab: edits the {@link GlassTheme} global design tokens live.
     */
    private final class ThemePanel extends Container implements LayoutDebugLabel {

        private final GlassSurface background = ModulePanel.this.glassPanel();
        private final TextNode title = textNode("Theme", TEXT);
        private final Container rows = new Container(new VerticalListLayout(0, true));
        private final ScrollContainer scroll = new LayoutDebugScrollContainer(this.rows);

        private ThemePanel() {
            super(gridLayout(0, 0));
            ModulePanel.this.configureScroll(this.scroll);
            place(this.background, fillCell(0, 0, 1, 2));
            place(this.title, cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING + 2F, 8F, PADDING, 0F), null, 30F));
            place(this.scroll, cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 2F, 0F, PADDING), null, null));
            this.addChild(this.background);
            this.addChild(this.title);
            this.addChild(this.scroll);
        }

        private void refresh() {
            this.rows.clearChildren();
            this.rows.addChild(new GroupHeader("Accent"));
            this.rows.addChild(new AccentRow());
            this.rows.addChild(new ColorStrip());
            this.rows.addChild(new GroupHeader("Glass"));
            this.rows.addChild(new SliderRow("Glass opacity", "How dense the glass panels are.",
                    15F, 95F, 1F, () -> GlassTheme.glassOpacity() * 100F,
                    value -> GlassTheme.glassOpacity(value / 100F),
                    () -> Math.round(GlassTheme.glassOpacity() * 100F) + "%"));
            this.rows.addChild(new SliderRow("Blur", "How strongly the panels blur the game behind them.",
                    0F, 10F, 1F, GlassTheme::glassBlur,
                    GlassTheme::glassBlur,
                    () -> Integer.toString(Math.round(GlassTheme.glassBlur()))));
            this.rows.addChild(new OptionRow("Background design", true, null,
                    () -> ModulePanel.this.backgroundDesign.displayName(),
                    ModulePanel.this::cycleBackgroundDesign, this::refresh));
            this.rows.addChild(new GroupHeader("Layout"));
            this.rows.addChild(new OptionRow("Compact cards", false,
                    () -> ModulePanel.this.compactRows,
                    () -> {
                        ModulePanel.this.compactRows = !ModulePanel.this.compactRows;
                        ModulePanel.this.saveUiPreferences();
                        ModulePanel.this.refreshModuleList();
                    }, this::refresh));
            this.rows.addChild(new OptionRow("Hover descriptions", false,
                    () -> ModulePanel.this.showSummaries,
                    () -> {
                        ModulePanel.this.showSummaries = !ModulePanel.this.showSummaries;
                        ModulePanel.this.saveUiPreferences();
                    }, this::refresh));
            this.rows.addChild(new OptionRow("Wide inspector", false,
                    () -> ModulePanel.this.wideInspector,
                    () -> {
                        ModulePanel.this.wideInspector = !ModulePanel.this.wideInspector;
                        ModulePanel.this.saveUiPreferences();
                    }, this::refresh));
            this.rows.addChild(new GroupHeader("Commands"));
            this.rows.addChild(new OptionRow("Command prefix", true, null,
                    CommandPrefix::get,
                    () -> {
                        CommandPrefix.cycle();
                        ModulePanel.this.config.save();
                    }, this::refresh));
            this.rows.addChild(new OptionRow("Reset UI preferences", false, null,
                    () -> "", ModulePanel.this::resetUiPreferences, this::refresh));
            ModulePanel.this.requestFrame();
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return constraints;
        }

        @Override
        public String layoutDebugLabel() {
            return "theme=" + GlassTheme.preset().key();
        }
    }

    /**
     * Accent preset swatches: one circle per preset, ringed when selected.
     */
    private final class AccentRow extends Container implements LayoutDebugLabel {

        private static final float SWATCH_BOX = 26F;

        private AccentRow() {
            super(gridLayout(4, 0));
            ClientConfig.GuiThemePreset[] presets = ClientConfig.GuiThemePreset.values();
            for (int index = 0; index < presets.length; index++) {
                AccentSwatch swatch = new AccentSwatch(presets[index]);
                place(swatch, cell(index, 0, 1, 1, 0F, 0F, GridAnchor.LEFT, GridFill.NONE,
                        padding(index == 0 ? PADDING : 0F, 0F, 0F, 0F), SWATCH_BOX, SWATCH_BOX));
                this.addChild(swatch);
            }
            this.fixedSize(-1, 36F);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), 36F);
        }

        @Override
        public String layoutDebugLabel() {
            return "accent=" + GlassTheme.preset().key();
        }
    }

    private final class AccentSwatch extends Component implements LayoutDebugLabel {

        private final ClientConfig.GuiThemePreset preset;
        private final Anim hover = new Anim();
        private boolean hovered;

        private AccentSwatch(final ClientConfig.GuiThemePreset preset) {
            this.preset = preset;
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            float centerX = size.width() / 2F;
            float centerY = size.height() / 2F;
            float radius = 8F + this.hover.toward(this.hovered ? 1F : 0F, 14F);
            if (this.preset == GlassTheme.preset()) {
                // Selection ring as a larger filled circle behind the swatch: both circles render
                // through the anti-aliased SDF path, unlike a tessellated outline.
                renderer.fillCircle(centerX, centerY, radius + 2F, TEXT);
            }
            renderer.fillCircle(centerX, centerY, radius, GuiThemePalette.of(this.preset).active());
        }

        @Override
        protected void onComponentMouseEnter() {
            this.hovered = true;
        }

        @Override
        protected void onComponentMouseLeave() {
            this.hovered = false;
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
            if (event.button() == MouseButton.LEFT) {
                ModulePanel.this.selectThemePreset(this.preset);
                return true;
            }
            return false;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(AccentRow.SWATCH_BOX, AccentRow.SWATCH_BOX);
        }

        @Override
        public String layoutDebugLabel() {
            return this.preset.key();
        }
    }

    /**
     * Label + live value + slider driving a global theme token.
     */
    private final class SliderRow extends Container implements LayoutDebugLabel {

        private final String name;
        private final TextNode label;
        private final TextNode description;
        private final TextNode value;
        private final Slider slider;

        private SliderRow(final String name, final String description, final float min, final float max, final float step,
                          final Supplier<Float> get, final Consumer<Float> set, final Supplier<String> display) {
            super(gridLayout(0, 0));
            this.name = name;
            this.label = textNode(name, MUTED);
            this.description = textNode(description, () -> FAINT);
            this.value = textNode(display, () -> TEXT)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.slider = slider(min, max, step, get.get());
            this.slider.valueChangeListener().add(newValue -> {
                set.accept(newValue.floatValue());
                ModulePanel.this.saveUiPreferences();
            });
            this.fixedSize(-1, NUMBER_ROW_HEIGHT);
            this.addChild(this.label);
            this.addChild(this.description);
            this.addChild(this.value);
            this.addChild(this.slider);
        }

        @Override
        public void computeLayout(final Size size) {
            float valueWidth = Math.min(58F, Math.max(0F, size.width() * 0.25F));
            place(this.label, cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 4F, 8F, 0F), null, 18F));
            place(this.value, cell(1, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 4F, PADDING, 0F), valueWidth, 18F));
            place(this.description, cell(0, 1, 2, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, PADDING, 0F), null, 17F));
            place(this.slider, cell(0, 2, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.HORIZONTAL,
                    padding(PADDING, 0F, PADDING, 0F), null, 16F));
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), NUMBER_ROW_HEIGHT);
        }

        @Override
        public String layoutDebugLabel() {
            return this.name;
        }
    }

    private final class SettingsDrawer extends Container implements LayoutDebugLabel {

        private static final float HEADER_HEIGHT = 38F;
        static final float ROW_HEIGHT = 36F;

        private final GlassSurface background = ModulePanel.this.glassDeepPanel();
        private final Surface headerDivider = surface(DIVIDER);
        private final IconButton backIcon = iconButton("chevron-left", () -> MUTED,
                ModulePanel.this::openRootDrawer,
                () -> ModulePanel.this.drawer != Drawer.ROOT && ModulePanel.this.drawer != Drawer.NONE);
        private final TextNode title = textNode(this::title, () -> TEXT);
        private final IconButton closeIcon = iconButton("x", () -> FAINT, () -> {
            ModulePanel.this.drawer = Drawer.NONE;
            ModulePanel.this.requestFrame();
        });
        private final Container body = new Container(new VerticalListLayout(0, true));

        private SettingsDrawer() {
            super(gridLayout(0, 4));
            place(this.background, fillCell(0, 0, 1, 2));
            place(this.headerDivider, cell(0, 0, 1, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    padding(PADDING, 0F, PADDING, 0F), null, 1F));
            place(this.backIcon, cell(0, 0, 1, 1, 0F, 0F, GridAnchor.LEFT, GridFill.NONE,
                    padding(6F, 0F, 0F, 0F), ICON_BUTTON_SIZE, ICON_BUTTON_SIZE));
            place(this.title, cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(38F, 0F, 38F, 0F), null, HEADER_HEIGHT));
            place(this.closeIcon, cell(0, 0, 1, 1, 0F, 0F, GridAnchor.RIGHT, GridFill.NONE,
                    padding(0F, 0F, 6F, 0F), ICON_BUTTON_SIZE, ICON_BUTTON_SIZE));
            place(this.body, cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 0F, PADDING), null, null));
            this.addChild(this.background);
            this.addChild(this.headerDivider);
            this.addChild(this.backIcon);
            this.addChild(this.title);
            this.addChild(this.closeIcon);
            this.addChild(this.body);
        }

        private void refresh() {
            this.body.clearChildren();
            switch (ModulePanel.this.drawer) {
                case ROOT -> this.buildRoot();
                case GENERAL -> this.buildGeneral();
                case MODULES -> this.buildModules();
                case SOUND -> this.buildSound();
                case NOTIFICATIONS -> this.buildNotifications();
                case NONE -> {
                }
            }
            ModulePanel.this.requestFrame();
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return constraints;
        }

        @Override
        public String layoutDebugLabel() {
            return ModulePanel.this.drawer.name().toLowerCase(Locale.ROOT);
        }

        private String title() {
            return switch (ModulePanel.this.drawer) {
                case ROOT -> "Settings";
                case GENERAL -> "General";
                case MODULES -> "Modules";
                case SOUND -> "Sound";
                case NOTIFICATIONS -> "Notifications";
                case NONE -> "";
            };
        }

        private void buildRoot() {
            this.addRow(new OptionRow("General", true, null, () -> "", () -> this.openNestedDrawer(Drawer.GENERAL), this::refresh));
            this.addRow(new OptionRow("Modules", true, null, () -> "", () -> this.openNestedDrawer(Drawer.MODULES), this::refresh));
            this.addRow(new OptionRow("Sound", true, null, () -> "", () -> this.openNestedDrawer(Drawer.SOUND), this::refresh));
            this.addRow(new OptionRow("Notifications", true, null, () -> "", () -> this.openNestedDrawer(Drawer.NOTIFICATIONS), this::refresh));
        }

        private void buildGeneral() {
            this.addRow(new OptionRow("Save config now", false, null, () -> "", ModulePanel.this::saveConfigNow, this::refresh));
            this.addRow(new OptionRow("Reload config", false, null, () -> "", ModulePanel.this::reloadConfig, this::refresh));
            this.addRow(new OptionRow("Reset UI preferences", false, null, () -> "", ModulePanel.this::resetUiPreferences, this::refresh));
            this.addRow(new OptionRow("Dump layout tree", false, null, () -> "", ModulePanel.this::dumpLayoutTree, this::refresh));
        }

        private void buildModules() {
            this.addRow(new OptionRow("Show disabled modules", false,
                    () -> ModulePanel.this.showDisabledModules,
                    () -> {
                        ModulePanel.this.showDisabledModules = !ModulePanel.this.showDisabledModules;
                        ModulePanel.this.saveUiPreferences();
                        ModulePanel.this.refreshModuleList();
                    }, this::refresh));
            this.addRow(new OptionRow("Enabled modules first", false,
                    () -> ModulePanel.this.enabledFirst,
                    () -> {
                        ModulePanel.this.enabledFirst = !ModulePanel.this.enabledFirst;
                        ModulePanel.this.saveUiPreferences();
                        ModulePanel.this.refreshModuleList();
                    }, this::refresh));
            this.addRow(new OptionRow("Reset module filters", false, null, () -> "",
                    ModulePanel.this::resetModuleListPreferences, this::refresh));
        }

        private void buildSound() {
            int rows = this.addShortcuts(
                    "sound_locator",
                    "sound_blocker",
                    "notebot",
                    "auto_fish",
                    "ambience"
            );
            if (rows == 0) {
                this.addBody(new EmptyState("No sound modules are registered."));
            }
        }

        private void buildNotifications() {
            this.addRow(new OptionRow("Toggle notifications", false,
                    ToggleNotifications::enabled,
                    () -> {
                        ToggleNotifications.enabled(!ToggleNotifications.enabled());
                        ModulePanel.this.config.save();
                    }, this::refresh));
            this.addRow(new OptionRow("Style", false, null,
                    () -> ToggleNotifications.mode().displayName(),
                    () -> {
                        ToggleNotifications.mode(ToggleNotifications.mode().next());
                        ModulePanel.this.config.save();
                    }, this::refresh));
            if (ToggleNotifications.mode() == ToggleNotifications.Mode.POPUP) {
                this.addRow(new OptionRow("Position", false, null,
                        () -> ToggleNotifications.corner().displayName(),
                        () -> {
                            ToggleNotifications.corner(ToggleNotifications.corner().next());
                            ModulePanel.this.config.save();
                        }, this::refresh));
                this.addRow(new OptionRow("Duration", false, null,
                        () -> (ToggleNotifications.durationMs() / 1000) + "s",
                        () -> {
                            ToggleNotifications.cycleDuration();
                            ModulePanel.this.config.save();
                        }, this::refresh));
            }
            this.addShortcuts(
                    "notifier",
                    "gamemode_notifier",
                    "lag_notifier_hud",
                    "staff_alert",
                    "auto_reconnect"
            );
        }

        private void addRow(final OptionRow row) {
            this.addBody(row);
        }

        private void addBody(final Component component) {
            this.body.addChild(component);
        }

        private void openNestedDrawer(final Drawer drawer) {
            ModulePanel.this.drawer = drawer;
            this.refresh();
        }

        private int addShortcuts(final String... moduleIds) {
            int rows = 0;
            for (String moduleId : moduleIds) {
                if (ModulePanel.this.addModuleShortcut(this.body, moduleId)) {
                    rows++;
                }
            }
            return rows;
        }
    }

    /**
     * Generic clickable option row: label, optional live value text, optional toggle, optional chevron.
     */
    private final class OptionRow extends Container implements LayoutDebugLabel {

        private final String text;
        private final Supplier<String> value;
        private final BooleanSupplier checked;
        private final Runnable action;
        private final Runnable afterAction;
        private final Surface divider = surface(DIVIDER);
        private final TextNode label;
        private final TextNode valueLabel;
        private final IconNode arrow;
        private final ToggleSwitch toggle;

        private OptionRow(final String text, final boolean arrow, final BooleanSupplier checked, final Runnable action,
                          final Runnable afterAction) {
            this(text, arrow, checked, () -> "", action, afterAction);
        }

        private OptionRow(final String text, final boolean arrow, final BooleanSupplier checked,
                          final Supplier<String> value, final Runnable action, final Runnable afterAction) {
            super(gridLayout(0, 0));
            this.text = text;
            this.value = value;
            this.checked = checked;
            this.action = action;
            this.afterAction = afterAction;
            this.label = textNode(text, () -> this.checked == null || this.checked() ? TEXT : MUTED);
            this.valueLabel = textNode(this.value, GlassTheme::accent)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.arrow = arrow ? iconNode("chevron-right", FAINT) : null;
            this.toggle = checked == null ? null : new ToggleSwitch(this::checked, this::runAction);
            this.fixedSize(-1, OPTION_ROW_HEIGHT);
            place(this.divider, cell(0, 1, 3, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    padding(PADDING, 0F, PADDING, 0F), null, 1F));
            place(this.label, cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, 8F, 0F), null, null));
            place(this.valueLabel, cell(1, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 4F, 0F), 86F, null));
            this.addChild(this.divider);
            this.addChild(this.label);
            this.addChild(this.valueLabel);
            if (this.arrow != null) {
                place(this.arrow, cell(2, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                        padding(0F, 0F, 8F, 0F), ICON_BOX, ICON_BOX));
                this.addChild(this.arrow);
            }
            if (this.toggle != null) {
                place(this.toggle, cell(2, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                        padding(0F, 0F, 14F, 0F), SWITCH_WIDTH, SWITCH_HEIGHT));
                this.addChild(this.toggle);
            }
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
            if (super.onComponentMouseDown(event, size)) {
                return true;
            }
            if (event.button() == MouseButton.LEFT) {
                this.runAction();
                return true;
            }
            return false;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), OPTION_ROW_HEIGHT);
        }

        @Override
        public String layoutDebugLabel() {
            return this.text;
        }

        private boolean checked() {
            return this.checked != null && this.checked.getAsBoolean();
        }

        private void runAction() {
            this.action.run();
            this.afterAction.run();
            ModulePanel.this.requestFrame();
        }
    }

    private final class ModuleShortcutRow extends Container implements LayoutDebugLabel {

        private final Module module;
        private final Surface divider = surface(DIVIDER);
        private final TextNode label;
        private final TextNode category;
        private final ToggleSwitch toggle;
        private final IconNode arrow = iconNode("chevron-right", FAINT);

        private ModuleShortcutRow(final Module module) {
            super(gridLayout(0, 0));
            this.module = module;
            this.label = textNode(module.name(), () -> this.module.enabled() ? TEXT : MUTED);
            this.category = textNode(module.category().displayName(), () -> FAINT)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.toggle = new ToggleSwitch(this.module::enabled, () -> ModulePanel.this.toggleModule(this.module));
            this.fixedSize(-1, SettingsDrawer.ROW_HEIGHT);
            place(this.divider, cell(0, 1, 4, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    padding(PADDING, 0F, PADDING, 0F), null, 1F));
            place(this.label, cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, 8F, 0F), null, null));
            place(this.category, cell(1, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 6F, 0F), 58F, null));
            place(this.toggle, cell(2, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 6F, 0F), SWITCH_WIDTH, SWITCH_HEIGHT));
            place(this.arrow, cell(3, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 8F, 0F), ICON_BOX, ICON_BOX));
            this.addChild(this.divider);
            this.addChild(this.label);
            this.addChild(this.category);
            this.addChild(this.toggle);
            this.addChild(this.arrow);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
            if (super.onComponentMouseDown(event, size)) {
                return true;
            }
            if (event.button() == MouseButton.LEFT) {
                ModulePanel.this.selectModuleShortcut(this.module);
                return true;
            }
            return false;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), SettingsDrawer.ROW_HEIGHT);
        }

        @Override
        public String layoutDebugLabel() {
            return this.module.id();
        }
    }

    private static final class ColorStrip extends Component implements LayoutDebugLabel {

        private static final List<Float> FRACTIONS = List.of(0.18F, 0.18F, 0.18F, 0.18F, 0.14F, 0.14F);

        private ColorStrip() {
            this.capabilities().all(false);
        }

        @Override
        public void render(final Renderer renderer, final Size size) {
            float x = PADDING;
            float width = Math.max(0F, size.width() - PADDING * 2F);
            renderer.fillRect(x, 0F, width, size.height(), TRACK);
            float offset = 0F;
            List<Color> colors = this.colors();
            for (int index = 0; index < colors.size(); index++) {
                float segmentWidth = width * FRACTIONS.get(index);
                renderer.fillRect(x + offset, 0F, segmentWidth, size.height(), colors.get(index));
                offset += segmentWidth;
            }
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), 2F);
        }

        @Override
        public String layoutDebugLabel() {
            return "theme";
        }

        private List<Color> colors() {
            return List.of(
                    Color.fromRGB(245, 56, 70),
                    Color.fromRGB(245, 132, 35),
                    Color.fromRGB(245, 204, 60),
                    GlassTheme.accent(),
                    Color.fromRGB(70, 116, 240),
                    Color.fromRGB(227, 70, 150)
            );
        }
    }

    private static final class EmptyState extends Container implements LayoutDebugLabel {

        private final String message;
        private final TextNode label;

        private EmptyState(final String message) {
            super(gridLayout(0, 0));
            this.message = message;
            this.fixedSize(-1, 64);
            this.label = textNode(message, FAINT);
            this.label.origin(TextOrigin.Horizontal.VISUAL_CENTER, TextOrigin.Vertical.LOGICAL_CENTER);
            place(this.label, cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(PADDING, 0F, PADDING, 0F), null, null));
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
}
