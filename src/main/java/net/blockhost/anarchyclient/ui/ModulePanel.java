package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.profile.ProfileManager;
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
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayout;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayoutOptions;
import net.lenni0451.rivet.layout.list.VerticalListLayout;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextFormat;
import net.lenni0451.rivet.text.model.TextLine;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.text.model.TextSection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class ModulePanel extends Container implements LayoutDebugLabel {

    private static final float TOP_BAR_HEIGHT = 40F;
    private static final float CATEGORY_WIDTH = 166F;
    private static final float INSPECTOR_WIDTH = 274F;
    private static final float INSPECTOR_WIDE_WIDTH = 310F;
    private static final float PADDING = 12F;
    private static final float SEARCH_HEIGHT = 28F;
    private static final float MODULE_ROW_HEIGHT = 46F;
    private static final float COMPACT_MODULE_ROW_HEIGHT = 36F;
    private static final float SETTING_ROW_HEIGHT = 34F;
    private static final float NUMBER_ROW_HEIGHT = 50F;
    private static final float TEXT_ROW_HEIGHT = 38F;
    private static final float DRAWER_WIDTH = 280F;
    private static final float CORNER_RADIUS = 2F;
    private static final float SWITCH_WIDTH = 24F;
    private static final float SWITCH_HEIGHT = 12F;
    private static final float ICON_BOX = 16F;
    // Lucide glyphs (font size 14) render with their visual center this far below the
    // top-left passed to the Minecraft font renderer. Single source for icon centering;
    // nudge by a pixel here if icons ever look slightly high or low.
    private static final float ICON_CENTER_OFFSET = 4F;

    private static final Color BACKDROP_TOP = Color.fromRGBA(0, 0, 0, 78);
    private static final Color BACKDROP_BOTTOM = Color.fromRGBA(0, 0, 0, 142);
    private static final Color SHADOW = Color.fromRGBA(0, 0, 0, 126);
    private static final Color SHELL = Color.fromRGBA(8, 8, 9, 236);
    private static final Color SURFACE = Color.fromRGBA(16, 16, 18, 236);
    private static final Color SURFACE_SOFT = Color.fromRGBA(20, 20, 23, 220);
    private static final Color SURFACE_HOVER = Color.fromRGBA(27, 27, 31, 236);
    private static final Color SURFACE_ACTIVE = Color.fromRGBA(25, 28, 28, 242);
    private static final Color FIELD = Color.fromRGBA(7, 7, 8, 230);
    private static final Color BORDER = Color.fromRGBA(48, 48, 54, 210);
    private static final Color BORDER_SOFT = Color.fromRGBA(34, 34, 39, 170);
    private static final Color TRACK = Color.fromRGBA(42, 42, 47, 230);
    private static final Color TEXT = Color.fromRGB(230, 230, 224);
    private static final Color MUTED = Color.fromRGB(142, 142, 138);
    private static final Color FAINT = Color.fromRGB(84, 84, 82);
    private static final Color ACTIVE = Color.fromRGB(0, 186, 148);
    private static final Color ACTIVE_SOFT = Color.fromRGBA(0, 186, 148, 72);
    private static final Color WARNING = Color.fromRGB(236, 142, 47);
    private static final FontDescription LUCIDE_FONT = new FontDescription.Resource(
            Identifier.fromNamespaceAndPath(AnarchyClient.MOD_ID, "lucide")
    );

    private final ModuleManager modules;
    private final ClientConfig config;
    private final TopBar topBar = new TopBar();
    private final CategoryRail categoryRail = new CategoryRail();
    private final SearchInput searchInput = new SearchInput("Search modules...");
    private final Container moduleRows = new Container(new VerticalListLayout(1, true));
    private final ScrollContainer moduleScroll = new ScrollContainer(this.moduleRows);
    private final ModuleInspector inspector = new ModuleInspector();
    private final FriendsPanel friendsPanel = new FriendsPanel();
    private final ProfilesPanel profilesPanel = new ProfilesPanel();
    private final SettingsDrawer settingsDrawer = new SettingsDrawer();

    private Tab selectedTab = Tab.MODULES;
    private ModuleCategory selectedCategory;
    private Module selectedModule;
    private Drawer drawer = Drawer.NONE;
    private String searchQuery = "";
    private boolean showDisabledModules = true;
    private boolean enabledFirst;
    private boolean showSummaries = true;
    private boolean compactRows;
    private boolean wideInspector;

    private float shellX;
    private float shellY;
    private float shellWidth;
    private float shellHeight;
    private float contentY;
    private float contentHeight;

    public ModulePanel(final ModuleManager modules, final ClientConfig config) {
        super(AbsoluteLayout.INSTANCE);
        this.modules = modules;
        this.config = config;
        this.selectedCategory = this.initialCategory();
        this.selectedModule = this.initialModule();
        this.searchInput.onChange(value -> {
            this.searchQuery = value.trim();
            this.refreshModuleList();
        });
        this.configureScroll(this.moduleScroll);
        this.addChild(this.topBar);
        this.addChild(this.categoryRail);
        this.addChild(this.searchInput);
        this.addChild(this.moduleScroll);
        this.addChild(this.inspector);
        this.addChild(this.friendsPanel);
        this.addChild(this.profilesPanel);
        this.addChild(this.settingsDrawer);
        this.refreshModuleList();
        this.inspector.refresh();
        this.friendsPanel.refresh();
        this.profilesPanel.refresh();
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        renderer.fillGradientRect(0, 0, bounds.width(), bounds.height(), BACKDROP_TOP, BACKDROP_BOTTOM, BACKDROP_BOTTOM, BACKDROP_TOP);
        renderer.fillRect(this.shellX + 3, this.shellY + 4, this.shellWidth, this.shellHeight, SHADOW);
        renderer.fillRect(this.shellX, this.shellY, this.shellWidth, this.shellHeight, SHELL);
        renderer.outlineRect(this.shellX, this.shellY, this.shellWidth, this.shellHeight, 1, BORDER);
        renderer.line(this.shellX, this.shellY + TOP_BAR_HEIGHT, this.shellX + this.shellWidth, this.shellY + TOP_BAR_HEIGHT, 1, BORDER_SOFT);
        if (this.selectedTab == Tab.MODULES) {
            float listX = this.shellX + CATEGORY_WIDTH;
            renderer.line(listX, this.contentY, listX, this.contentY + this.contentHeight, 1, BORDER_SOFT);
            float inspectorX = this.inspectorX();
            if (this.inspectorVisible()) {
                renderer.line(inspectorX, this.contentY, inspectorX, this.contentY + this.contentHeight, 1, BORDER_SOFT);
            }
        }
        super.render(renderer, bounds);
    }

    @Override
    public void computeLayout(final Size size) {
        this.shellWidth = clamp(size.width() - 56F, 620F, 860F);
        this.shellHeight = clamp(size.height() - 58F, 370F, 490F);
        this.shellX = Math.max(16F, (size.width() - this.shellWidth) / 2F);
        this.shellY = Math.max(18F, (size.height() - this.shellHeight) / 2F);
        this.contentY = this.shellY + TOP_BAR_HEIGHT;
        this.contentHeight = this.shellHeight - TOP_BAR_HEIGHT;

        this.topBar.layoutOptions(new AbsoluteLayoutOptions(this.shellX, this.shellY, this.shellWidth, TOP_BAR_HEIGHT));
        if (this.selectedTab == Tab.MODULES) {
            this.layoutModules();
        } else {
            this.hide(this.categoryRail);
            this.hide(this.searchInput);
            this.hide(this.moduleScroll);
            this.hide(this.inspector);
            this.layoutTabPanel(this.selectedTab == Tab.FRIENDS ? this.friendsPanel : this.profilesPanel);
            this.hide(this.selectedTab == Tab.FRIENDS ? this.profilesPanel : this.friendsPanel);
        }
        if (this.drawer == Drawer.NONE) {
            this.hide(this.settingsDrawer);
        } else {
            float width = Math.min(DRAWER_WIDTH, this.shellWidth - 180F);
            this.settingsDrawer.layoutOptions(new AbsoluteLayoutOptions(
                    this.shellX + this.shellWidth - width,
                    this.contentY,
                    width,
                    this.contentHeight
            ));
        }
        super.computeLayout(size);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return constraints;
    }

    @Override
    public String layoutDebugLabel() {
        String category = this.selectedCategory == null ? "none" : this.selectedCategory.name().toLowerCase(Locale.ROOT);
        String module = this.selectedModule == null ? "none" : this.selectedModule.id();
        return "tab=" + this.selectedTab.name().toLowerCase(Locale.ROOT)
                + " category=" + category
                + " module=" + module
                + " drawer=" + this.drawer.name().toLowerCase(Locale.ROOT);
    }

    private void layoutModules() {
        float inspectorWidth = this.inspectorVisible() ? this.currentInspectorWidth() : 0F;
        float listX = this.shellX + CATEGORY_WIDTH;
        float listWidth = this.shellWidth - CATEGORY_WIDTH - inspectorWidth;
        this.categoryRail.layoutOptions(new AbsoluteLayoutOptions(this.shellX, this.contentY, CATEGORY_WIDTH, this.contentHeight));
        this.searchInput.layoutOptions(new AbsoluteLayoutOptions(
                listX + PADDING,
                this.contentY + PADDING,
                Math.max(0, listWidth - PADDING * 2),
                SEARCH_HEIGHT
        ));
        this.moduleScroll.layoutOptions(new AbsoluteLayoutOptions(
                listX + PADDING,
                this.contentY + PADDING + SEARCH_HEIGHT + 10F,
                Math.max(0, listWidth - PADDING * 2),
                Math.max(0, this.contentHeight - PADDING * 2 - SEARCH_HEIGHT - 10F)
        ));
        if (this.inspectorVisible()) {
            this.inspector.layoutOptions(new AbsoluteLayoutOptions(this.inspectorX(), this.contentY, inspectorWidth, this.contentHeight));
        } else {
            this.hide(this.inspector);
        }
        this.hide(this.friendsPanel);
        this.hide(this.profilesPanel);
    }

    private void layoutTabPanel(final Component panel) {
        panel.layoutOptions(new AbsoluteLayoutOptions(
                this.shellX + PADDING,
                this.contentY + PADDING,
                this.shellWidth - PADDING * 2,
                this.contentHeight - PADDING * 2
        ));
    }

    private boolean inspectorVisible() {
        return this.shellWidth >= 730F;
    }

    private float currentInspectorWidth() {
        return this.wideInspector ? INSPECTOR_WIDE_WIDTH : INSPECTOR_WIDTH;
    }

    private float inspectorX() {
        return this.shellX + this.shellWidth - this.currentInspectorWidth();
    }

    private void hide(final Component component) {
        component.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, 0F, 0F));
    }

    private void selectTab(final Tab tab) {
        if (this.selectedTab == tab) {
            return;
        }
        this.selectedTab = tab;
        this.drawer = Drawer.NONE;
        if (tab == Tab.FRIENDS) {
            this.friendsPanel.refresh();
        } else if (tab == Tab.PROFILES) {
            this.profilesPanel.refresh();
        }
        this.requestFrame();
    }

    private void selectCategory(final ModuleCategory category) {
        if (this.selectedCategory == category) {
            return;
        }
        this.selectedCategory = category;
        this.config.selectedCategory(category);
        this.selectedModule = this.firstVisibleModule();
        this.saveSelectedModule();
        this.refreshModuleList();
        this.inspector.refresh();
        this.requestFrame();
    }

    private void selectModule(final Module module) {
        if (this.selectedModule == module) {
            return;
        }
        this.selectedModule = module;
        this.selectedCategory = module.category();
        this.config.selectedCategory(this.selectedCategory);
        this.saveSelectedModule();
        this.refreshModuleList();
        this.inspector.refresh();
        this.requestFrame();
    }

    private void toggleModule(final Module module) {
        module.toggle();
        this.config.save();
        this.refreshModuleList();
        this.inspector.refresh();
        this.requestFrame();
    }

    private void openDrawer(final Drawer drawer) {
        this.drawer = this.drawer == drawer ? Drawer.NONE : drawer;
        this.settingsDrawer.refresh();
        this.requestFrame();
    }

    private void saveSelectedModule() {
        if (this.selectedModule != null) {
            this.config.selectedModuleId(this.selectedModule.id());
        }
    }

    private void refreshModuleList() {
        this.moduleRows.clearChildren();
        List<Module> visible = this.visibleModules();
        if (!visible.contains(this.selectedModule)) {
            this.selectedModule = visible.isEmpty() ? null : visible.getFirst();
            this.saveSelectedModule();
        }
        if (visible.isEmpty()) {
            this.moduleRows.addChild(new EmptyState("No modules match the current filter."));
        } else {
            for (Module module : visible) {
                this.moduleRows.addChild(new ModuleRow(module));
            }
        }
        this.inspector.refresh();
        this.requestFrame();
    }

    private List<Module> visibleModules() {
        List<Module> result = new ArrayList<>(this.modules.byCategory(this.selectedCategory));
        String query = this.searchQuery.toLowerCase(Locale.ROOT);
        result.removeIf(module -> {
            if (!this.showDisabledModules && !module.enabled()) {
                return true;
            }
            return !query.isEmpty() && !matchesQuery(module, query);
        });
        if (this.enabledFirst) {
            result.sort(Comparator.comparing((Module module) -> Boolean.valueOf(module.enabled()))
                    .reversed()
                    .thenComparing(Module::name, String.CASE_INSENSITIVE_ORDER));
        }
        return result;
    }

    private Module firstVisibleModule() {
        List<Module> visible = this.visibleModules();
        return visible.isEmpty() ? null : visible.getFirst();
    }

    private ModuleCategory initialCategory() {
        ModuleCategory fallback = this.firstNonEmptyCategory();
        return this.config.selectedCategory()
                .filter(category -> !this.modules.byCategory(category).isEmpty())
                .orElse(fallback);
    }

    private Module initialModule() {
        return this.config.selectedModuleId()
                .flatMap(this.modules::find)
                .orElseGet(() -> this.modules.byCategory(this.selectedCategory).stream().findFirst().orElse(null));
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
        scroll.barColor().set(Color.fromRGBA(122, 122, 122, 58));
        scroll.barHoverColor().set(Color.fromRGBA(146, 146, 146, 96));
        scroll.barClickColor().set(ACTIVE.multiplyAlpha(0.72F));
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

    private static boolean matchesQuery(final Module module, final String query) {
        if (module.name().toLowerCase(Locale.ROOT).contains(query) || module.id().contains(query)) {
            return true;
        }
        for (String alias : module.aliases()) {
            if (alias.toLowerCase(Locale.ROOT).contains(query)) {
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

    private static String moduleSummary(final Module module) {
        List<String> parts = new ArrayList<>();
        for (Setting<?> setting : module.settings()) {
            if (!setting.visible()) {
                continue;
            }
            String value = SettingControls.displayValue(setting);
            if (setting instanceof BooleanSetting bool) {
                if (bool.value()) {
                    parts.add(setting.name());
                }
            } else if (!value.isBlank()) {
                parts.add(value + " " + setting.name());
            }
            if (parts.size() >= 4) {
                break;
            }
        }
        return parts.isEmpty() ? "No visible settings" : abbreviate(String.join(", ", parts), 82);
    }

    private static Component settingRow(final Setting<?> setting, final Runnable rebuild, final Runnable save) {
        if (setting instanceof BooleanSetting bool) {
            return new BooleanSettingRow(bool, rebuild);
        }
        if (setting instanceof NumberSetting number) {
            return new NumberSettingRow(number, save);
        }
        if (setting instanceof SelectSetting select) {
            return new SelectSettingRow(select, rebuild);
        }
        if (setting instanceof TextValueSetting text) {
            return new TextSettingRow(setting, text, save);
        }
        return new ValueSettingRow(setting);
    }

    private static String abbreviate(final String value, final int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).stripTrailing() + "...";
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

        int low = 0;
        int high = value.length();
        while (low < high) {
            int middle = (low + high + 1) / 2;
            String candidate = value.substring(0, middle).stripTrailing() + ellipsis;
            if (textWidth(component, candidate, color) <= maxWidth) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return value.substring(0, low).stripTrailing() + ellipsis;
    }

    private static float textWidth(final Component component, final String value, final Color color) {
        return component.rivet().backend().shapeText(value, color).logicalBounds().width();
    }

    private static float clamp(final float value, final float min, final float max) {
        return (float) MathUtils.clamp(value, min, max);
    }

    private static FormattedLabel label(final String text, final Color color) {
        FormattedLabel label = new FormattedLabel(new TextLine(new TextSection(text, TextFormat.DEFAULT.withColor(color))));
        label.horizontalOrigin(TextOrigin.Horizontal.LOGICAL_LEFT);
        label.verticalOrigin(TextOrigin.Vertical.LOGICAL_CENTER);
        label.interactive(false);
        return label;
    }

    private static Button button(final String text, final Color color, final Button.ClickListener listener) {
        Button button = new Button(label(text, color), listener);
        button.cornerRadius().set(CORNER_RADIUS);
        button.outlineWidth().set(1F);
        button.inactiveColor().set(SURFACE_SOFT);
        button.inactiveOutlineColor().set(BORDER_SOFT);
        button.activeColor().set(SURFACE_HOVER);
        button.activeOutlineColor().set(BORDER);
        button.clickColor().set(FIELD);
        button.clickOutlineColor().set(ACTIVE);
        button.innerPadding().set(new Padding(8, 4, 8, 4));
        button.clickOn().set(Button.ClickOn.UP);
        return button;
    }

    private static TextField textField(final String value) {
        TextField field = new TextField(value);
        field.backgroundColor().set(FIELD);
        field.outlineColor().set(BORDER_SOFT);
        field.focusedOutlineColor().set(ACTIVE);
        field.selectionColor().set(ACTIVE.multiplyAlpha(0.4F));
        field.cursorColor().set(TEXT);
        field.cornerRadius().set(CORNER_RADIUS);
        field.innerPadding().set(new Padding(7, 4, 7, 4));
        return field;
    }

    private static Slider slider(final NumberSetting setting) {
        Slider slider = new Slider(setting.min(), setting.max(), setting.step(), setting.value());
        slider.barColor().set(TRACK);
        slider.activeBarColor().set(ACTIVE);
        slider.thumbColor().set(ACTIVE);
        slider.thumbClickColor().set(TEXT);
        slider.barHeight().set(3F);
        slider.barCornerRadius().set(1F);
        slider.thumbWidth().set(9F);
        slider.thumbHeight().set(9F);
        slider.thumbCornerRadius().set(2F);
        slider.fixedSize(new Size(-1, 16));
        return slider;
    }

    private static void drawText(final Component component, final Renderer renderer, final String text, final Color color,
                                 final float x, final float y, final TextOrigin.Horizontal horizontal,
                                 final TextOrigin.Vertical vertical) {
        renderer.text(component.rivet().backend().shapeText(text, color), x, y, horizontal, vertical);
    }

    /**
     * Draws a Lucide icon centered on {@code (centerX, centerY)}. The X axis is centered exactly
     * using the measured glyph width; the Y axis uses the single {@link #ICON_CENTER_OFFSET}
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
        float top = centerY - ICON_CENTER_OFFSET;
        renderer.custom((GuiGraphicsExtractor graphics) -> graphics.text(
                Minecraft.getInstance().font,
                component,
                Math.round(left),
                Math.round(top),
                argb(color),
                false
        ), new Rectangle(left, centerY - ICON_BOX / 2F, width, ICON_BOX));
    }

    private static void drawFittedText(final Component component, final Renderer renderer, final String text, final Color color,
                                       final float x, final float y, final float maxWidth,
                                       final TextOrigin.Horizontal horizontal, final TextOrigin.Vertical vertical) {
        String fitted = fitText(component, text, color, maxWidth);
        if (!fitted.isEmpty()) {
            drawText(component, renderer, fitted, color, x, y, horizontal, vertical);
        }
    }

    private static void drawSwitch(final Renderer renderer, final float x, final float y, final boolean checked) {
        Color track = checked ? ACTIVE : TRACK;
        float height = SWITCH_HEIGHT;
        float radius = height / 2F;
        renderer.optimizedFillRoundedRect(x, y, SWITCH_WIDTH, height, radius, track);
        renderer.fillCircle(x + (checked ? SWITCH_WIDTH - radius : radius), y + radius, radius - 2F, checked ? SHELL : MUTED);
    }

    /**
     * A reusable pill toggle. Owning its own bounds and click handling means the painted switch and
     * its clickable area can never drift apart, which was the cause of the misplaced toggles.
     */
    private static final class ToggleSwitch extends Component {

        private final BooleanSupplier state;
        private final Runnable onToggle;

        private ToggleSwitch(final BooleanSupplier state, final Runnable onToggle) {
            this.state = state;
            this.onToggle = onToggle;
            this.fixedSize(new Size(SWITCH_WIDTH, SWITCH_HEIGHT));
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            drawSwitch(renderer, 0, 0, this.state.getAsBoolean());
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() == MouseButton.LEFT) {
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
        MODULES("Modules"),
        FRIENDS("Friends"),
        PROFILES("Profiles");

        private final String label;

        Tab(final String label) {
            this.label = label;
        }
    }

    private enum Drawer {
        NONE,
        ROOT,
        MODULES,
        GUI
    }

    private final class TopBar extends Component {

        private static final float REFRESH_ICON_X_FROM_RIGHT = 50F;
        private static final float SETTINGS_ICON_X_FROM_RIGHT = 24F;

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SHELL);
            float centerY = bounds.height() / 2F;
            drawText(this, renderer, "ANARCHY", TEXT, 14, centerY, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            float clientX = 14F + textWidth(this, "ANARCHY", TEXT) + 6F;
            drawText(this, renderer, "client", ACTIVE, clientX, centerY, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);

            float tabX = 144F;
            for (Tab tab : Tab.values()) {
                float width = this.tabWidth(tab);
                Color color = ModulePanel.this.selectedTab == tab ? TEXT : FAINT;
                drawText(this, renderer, tab.label, color, tabX, centerY, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
                if (ModulePanel.this.selectedTab == tab) {
                    renderer.fillRect(tabX, bounds.height() - 6F, width - 8F, 2F, ACTIVE);
                }
                tabX += width;
            }

            drawIcon(renderer, "refresh-cw", bounds.width() - REFRESH_ICON_X_FROM_RIGHT, centerY, ModulePanel.this.drawer == Drawer.NONE ? FAINT : MUTED);
            drawIcon(renderer, "settings", bounds.width() - SETTINGS_ICON_X_FROM_RIGHT, centerY, ModulePanel.this.drawer == Drawer.NONE ? MUTED : ACTIVE);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() != MouseButton.LEFT) {
                return false;
            }
            float x = event.x();
            float tabX = 144F;
            for (Tab tab : Tab.values()) {
                float width = this.tabWidth(tab);
                if (x >= tabX && x <= tabX + width - 10F && event.y() >= 8F && event.y() <= bounds.height()) {
                    ModulePanel.this.selectTab(tab);
                    return true;
                }
                tabX += width;
            }
            if (this.iconHit(event, x, bounds.width() - SETTINGS_ICON_X_FROM_RIGHT, bounds.height())) {
                ModulePanel.this.openDrawer(Drawer.ROOT);
                return true;
            }
            if (this.iconHit(event, x, bounds.width() - REFRESH_ICON_X_FROM_RIGHT, bounds.height())) {
                ModulePanel.this.refreshModuleList();
                ModulePanel.this.inspector.refresh();
                return true;
            }
            return false;
        }

        private boolean iconHit(final MouseButtonEvent event, final float x, final float centerX, final float barHeight) {
            return Math.abs(x - centerX) <= ICON_BOX / 2F + 3F
                    && Math.abs(event.y() - barHeight / 2F) <= ICON_BOX / 2F + 4F;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), TOP_BAR_HEIGHT);
        }

        private float tabWidth(final Tab tab) {
            return switch (tab) {
                case MODULES -> 78F;
                case FRIENDS -> 72F;
                case PROFILES -> 82F;
            };
        }
    }

    private final class CategoryRail extends Component {

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SHELL);
            drawText(this, renderer, "Modules", TEXT, 22, 28, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            float y = 56F;
            for (ModuleCategory category : ModuleCategory.values()) {
                boolean selected = ModulePanel.this.selectedCategory == category;
                int enabled = ModulePanel.this.enabledCount(category);
                float rowCenter = y + 17F;
                if (selected) {
                    renderer.fillRect(10, y, bounds.width() - 20F, 34F, SURFACE_ACTIVE);
                    renderer.fillRect(10, y, 3F, 34F, ACTIVE);
                }
                Color textColor = selected ? TEXT : MUTED;
                drawIcon(renderer, categoryIcon(category), 24F, rowCenter, selected ? ACTIVE : FAINT);
                drawFittedText(this, renderer, category.displayName(), textColor, 42, rowCenter,
                        Math.max(0F, bounds.width() - 78F), TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
                if (enabled > 0) {
                    drawText(this, renderer, String.valueOf(enabled), selected ? ACTIVE : FAINT, bounds.width() - 24F, rowCenter,
                            TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
                }
                y += 40F;
            }
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() != MouseButton.LEFT) {
                return false;
            }
            float y = 56F;
            for (ModuleCategory category : ModuleCategory.values()) {
                if (event.y() >= y && event.y() <= y + 34F) {
                    ModulePanel.this.selectCategory(category);
                    return true;
                }
                y += 40F;
            }
            return false;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return constraints;
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

    private final class SearchInput extends Container implements LayoutDebugLabel {

        private final String placeholder;
        private final TextField field = textField("");
        private Consumer<String> changeListener = ignored -> {
        };

        private SearchInput(final String placeholder) {
            super(AbsoluteLayout.INSTANCE);
            this.placeholder = placeholder;
            this.field.backgroundColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.outlineColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.focusedOutlineColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.valueChangeListener().add(value -> this.changeListener.accept(value));
            this.addChild(this.field);
        }

        private void onChange(final Consumer<String> listener) {
            this.changeListener = listener;
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.optimizedFillRoundedRect(0, 0, bounds.width(), bounds.height(), CORNER_RADIUS, FIELD);
            renderer.optimizedOutlineRoundedRect(0, 0, bounds.width(), bounds.height(), CORNER_RADIUS, 1, BORDER);
            drawIcon(renderer, "search", 13F, bounds.height() / 2F, FAINT);
            if (this.field.text().isEmpty()) {
                drawText(this, renderer, this.placeholder, FAINT, 26, bounds.height() / 2F,
                        TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            }
            super.render(renderer, bounds);
        }

        @Override
        public void computeLayout(final Size size) {
            this.field.layoutOptions(new AbsoluteLayoutOptions(22, 2, Math.max(0, size.width() - 26), Math.max(0, size.height() - 4)));
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), SEARCH_HEIGHT);
        }

        @Override
        public String layoutDebugLabel() {
            return this.field.text().isBlank() ? "empty" : this.field.text();
        }
    }

    private final class ModuleRow extends Container implements LayoutDebugLabel {

        private static final float SWITCH_X_FROM_RIGHT = 58F;
        private static final float KEBAB_X_FROM_RIGHT = 22F;

        private final Module module;
        private final ToggleSwitch toggle;
        private boolean hovered;

        private ModuleRow(final Module module) {
            super(AbsoluteLayout.INSTANCE);
            this.module = module;
            this.toggle = new ToggleSwitch(module::enabled, () -> ModulePanel.this.toggleModule(module));
            this.fixedSize(new Size(-1, ModulePanel.this.compactRows ? COMPACT_MODULE_ROW_HEIGHT : MODULE_ROW_HEIGHT));
            this.addChild(this.toggle);
        }

        @Override
        protected void onComponentMouseEnter() {
            this.hovered = true;
        }

        @Override
        protected void onComponentMouseLeave() {
            this.hovered = false;
            super.onComponentMouseLeave();
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            boolean selected = this.module == ModulePanel.this.selectedModule;
            Color background = selected ? SURFACE_ACTIVE : this.hovered ? SURFACE_HOVER : SURFACE;
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), background);
            if (selected || this.module.enabled()) {
                renderer.fillRect(0, 0, 3, bounds.height(), selected ? ACTIVE : ACTIVE.multiplyAlpha(0.55F));
            }
            Color nameColor = this.module.enabled() ? TEXT : MUTED;
            float controlsX = bounds.width() - 66F;
            float summaryX = 152F;
            boolean twoLine = ModulePanel.this.showSummaries && !ModulePanel.this.compactRows;
            float nameMaxWidth = twoLine ? summaryX - 28F : controlsX - 28F;
            float nameCenterY = twoLine ? bounds.height() / 2F - 7F : bounds.height() / 2F;
            drawFittedText(this, renderer, this.module.name(), nameColor, 18, nameCenterY,
                    Math.max(0F, nameMaxWidth), TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            if (twoLine) {
                drawFittedText(this, renderer, moduleSummary(this.module), FAINT, summaryX, bounds.height() / 2F + 7F,
                        Math.max(0F, controlsX - summaryX - 16F),
                        TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            }
            drawIcon(renderer, "more-vertical", bounds.width() - KEBAB_X_FROM_RIGHT, bounds.height() / 2F, FAINT);
            super.render(renderer, bounds);
        }

        @Override
        public void computeLayout(final Size size) {
            this.toggle.layoutOptions(new AbsoluteLayoutOptions(
                    size.width() - SWITCH_X_FROM_RIGHT, (size.height() - SWITCH_HEIGHT) / 2F, SWITCH_WIDTH, SWITCH_HEIGHT));
            super.computeLayout(size);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() != MouseButton.LEFT) {
                return false;
            }
            if (super.onComponentMouseDown(event, bounds)) {
                return true;
            }
            ModulePanel.this.selectModule(this.module);
            return true;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), ModulePanel.this.compactRows ? COMPACT_MODULE_ROW_HEIGHT : MODULE_ROW_HEIGHT);
        }

        @Override
        public String layoutDebugLabel() {
            return this.module.id();
        }
    }

    private final class ModuleInspector extends Container implements LayoutDebugLabel {

        private final InspectorHeader header = new InspectorHeader();
        private final Container settings = new Container(new VerticalListLayout(0, true));
        private final ScrollContainer scroll = new ScrollContainer(this.settings);

        private ModuleInspector() {
            super(AbsoluteLayout.INSTANCE);
            ModulePanel.this.configureScroll(this.scroll);
            this.addChild(this.header);
            this.addChild(this.scroll);
        }

        private void refresh() {
            this.settings.clearChildren();
            Module module = ModulePanel.this.selectedModule;
            if (module == null) {
                this.settings.addChild(new EmptyState("Select a module to edit its settings."));
            } else {
                List<SettingGroup> groups = visibleSettingGroups(module);
                boolean groupHeaders = showGroupHeaders(groups);
                for (SettingGroup group : groups) {
                    if (groupHeaders) {
                        this.settings.addChild(new GroupHeader(group.name()));
                    }
                    for (Setting<?> setting : group.settings()) {
                        if (setting.visible()) {
                            this.settings.addChild(settingRow(setting, this::refreshAfterSettingChange, ModulePanel.this.config::save));
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
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SURFACE);
            super.render(renderer, bounds);
        }

        @Override
        public void computeLayout(final Size size) {
            float headerHeight = 86F;
            this.header.layoutOptions(new AbsoluteLayoutOptions(0, 0, size.width(), headerHeight));
            this.scroll.layoutOptions(new AbsoluteLayoutOptions(0, headerHeight, size.width(), Math.max(0, size.height() - headerHeight)));
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return constraints;
        }

        @Override
        public String layoutDebugLabel() {
            return ModulePanel.this.selectedModule == null ? "empty" : ModulePanel.this.selectedModule.id();
        }
    }

    private final class InspectorHeader extends Container implements LayoutDebugLabel {

        private static final float TITLE_CENTER_Y = 30F;
        private static final float TOGGLE_X_FROM_RIGHT = 40F;
        private static final float ON_LABEL_X_FROM_RIGHT = 48F;

        private final ToggleSwitch toggle;

        private InspectorHeader() {
            super(AbsoluteLayout.INSTANCE);
            this.toggle = new ToggleSwitch(
                    () -> ModulePanel.this.selectedModule != null && ModulePanel.this.selectedModule.enabled(),
                    () -> {
                        if (ModulePanel.this.selectedModule != null) {
                            ModulePanel.this.toggleModule(ModulePanel.this.selectedModule);
                        }
                    });
            this.addChild(this.toggle);
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            Module module = ModulePanel.this.selectedModule;
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SURFACE);
            renderer.line(0, bounds.height() - 1, bounds.width(), bounds.height() - 1, 1, BORDER_SOFT);
            if (module == null) {
                drawText(this, renderer, "No module", MUTED, 16, bounds.height() / 2F,
                        TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
                return;
            }
            drawFittedText(this, renderer, module.name(), TEXT, 16, TITLE_CENTER_Y,
                    Math.max(0F, bounds.width() - 112F), TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            drawFittedText(this, renderer, module.category().displayName(), FAINT, 16, 52F,
                    Math.max(0F, bounds.width() - 82F), TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            String onLabel = module.enabled() ? "ON" : "OFF";
            Color onColor = module.enabled() ? ACTIVE : FAINT;
            float onX = bounds.width() - ON_LABEL_X_FROM_RIGHT;
            drawText(this, renderer, onLabel, onColor, onX, TITLE_CENTER_Y,
                    TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            float starX = onX - textWidth(this, onLabel, onColor) - 14F;
            drawIcon(renderer, "star", starX, TITLE_CENTER_Y, module.enabled() ? WARNING : FAINT);
            super.render(renderer, bounds);
        }

        @Override
        public void computeLayout(final Size size) {
            if (ModulePanel.this.selectedModule != null) {
                this.toggle.layoutOptions(new AbsoluteLayoutOptions(
                        size.width() - TOGGLE_X_FROM_RIGHT, TITLE_CENTER_Y - SWITCH_HEIGHT / 2F, SWITCH_WIDTH, SWITCH_HEIGHT));
            } else {
                this.toggle.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, 0F, 0F));
            }
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), 86F);
        }

        @Override
        public String layoutDebugLabel() {
            return ModulePanel.this.selectedModule == null ? "empty" : ModulePanel.this.selectedModule.id();
        }
    }

    private static final class GroupHeader extends Component implements LayoutDebugLabel {

        private final String name;

        private GroupHeader(final String name) {
            this.name = name;
            this.fixedSize(new Size(-1, 24));
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SHELL);
            drawText(this, renderer, this.name, MUTED, 12, bounds.height() / 2F, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
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

    private abstract static class SettingLine extends Component implements LayoutDebugLabel {

        private final Setting<?> setting;
        private final float height;

        private SettingLine(final Setting<?> setting, final float height) {
            this.setting = setting;
            this.height = height;
            this.fixedSize(new Size(-1, height));
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SURFACE);
            renderer.line(0, bounds.height() - 1, bounds.width(), bounds.height() - 1, 1, BORDER_SOFT);
            drawFittedText(this, renderer, this.setting.name(), MUTED, 12, bounds.height() / 2F,
                    Math.max(0F, this.labelMaxWidth(bounds)), TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), this.height);
        }

        protected float labelMaxWidth(final Rectangle bounds) {
            return bounds.width() - 64F;
        }

        @Override
        public String layoutDebugLabel() {
            return this.setting.id();
        }
    }

    private static final class BooleanSettingRow extends SettingLine {

        private final BooleanSetting setting;
        private final Runnable onChange;

        private BooleanSettingRow(final BooleanSetting setting, final Runnable onChange) {
            super(setting, SETTING_ROW_HEIGHT);
            this.setting = setting;
            this.onChange = onChange;
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            super.render(renderer, bounds);
            drawSwitch(renderer, bounds.width() - 38, bounds.height() / 2F - SWITCH_HEIGHT / 2F, this.setting.value());
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() == MouseButton.LEFT) {
                this.setting.value(!this.setting.value());
                this.onChange.run();
                return true;
            }
            return false;
        }
    }

    private static final class SelectSettingRow extends SettingLine {

        private final SelectSetting setting;
        private final Runnable onChange;

        private SelectSettingRow(final SelectSetting setting, final Runnable onChange) {
            super(setting, SETTING_ROW_HEIGHT);
            this.setting = setting;
            this.onChange = onChange;
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            super.render(renderer, bounds);
            String value = fitText(this, this.setting.value(), TEXT, Math.min(100F, Math.max(0F, bounds.width() * 0.42F)));
            drawText(this, renderer, value, TEXT, bounds.width() - 28, bounds.height() / 2F,
                    TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            drawIcon(renderer, "chevron-down", bounds.width() - 20F, bounds.height() / 2F, FAINT);
        }

        @Override
        protected float labelMaxWidth(final Rectangle bounds) {
            return bounds.width() - Math.min(130F, Math.max(80F, bounds.width() * 0.46F));
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
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

        private ValueSettingRow(final Setting<?> setting) {
            super(setting, SETTING_ROW_HEIGHT);
            this.setting = setting;
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            super.render(renderer, bounds);
            String value = fitText(this, SettingControls.displayValue(this.setting), TEXT, Math.min(112F, Math.max(0F, bounds.width() * 0.44F)));
            drawText(this, renderer, value, TEXT, bounds.width() - 12,
                    bounds.height() / 2F, TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        }

        @Override
        protected float labelMaxWidth(final Rectangle bounds) {
            return bounds.width() - Math.min(136F, Math.max(84F, bounds.width() * 0.48F));
        }
    }

    private static final class NumberSettingRow extends Container implements LayoutDebugLabel {

        private final NumberSetting setting;
        private final Slider slider;
        private final Runnable save;

        private NumberSettingRow(final NumberSetting setting, final Runnable save) {
            super(AbsoluteLayout.INSTANCE);
            this.setting = setting;
            this.save = save;
            this.slider = slider(setting);
            this.slider.valueChangeListener().add(value -> {
                setting.value(value);
                this.save.run();
            });
            this.fixedSize(new Size(-1, NUMBER_ROW_HEIGHT));
            this.addChild(this.slider);
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SURFACE);
            renderer.line(0, bounds.height() - 1, bounds.width(), bounds.height() - 1, 1, BORDER_SOFT);
            drawFittedText(this, renderer, this.setting.name(), MUTED, 12, 15,
                    Math.max(0F, bounds.width() - 74F), TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            drawFittedText(this, renderer, SettingControls.displayValue(this.setting), TEXT, bounds.width() - 12, 15,
                    Math.min(58F, Math.max(0F, bounds.width() * 0.25F)),
                    TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            super.render(renderer, bounds);
        }

        @Override
        public void computeLayout(final Size size) {
            this.slider.layoutOptions(new AbsoluteLayoutOptions(12F, 28F, Math.max(0F, size.width() - 24F), 16F));
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), NUMBER_ROW_HEIGHT);
        }

        @Override
        public String layoutDebugLabel() {
            return this.setting.id();
        }
    }

    private static final class TextSettingRow extends Container implements LayoutDebugLabel {

        private final Setting<?> setting;
        private final TextValueSetting textSetting;
        private final TextField field;
        private final Runnable save;

        private TextSettingRow(final Setting<?> setting, final TextValueSetting textSetting, final Runnable save) {
            super(AbsoluteLayout.INSTANCE);
            this.setting = setting;
            this.textSetting = textSetting;
            this.save = save;
            this.field = textField(textSetting.valueString());
            this.field.valueChangeListener().add(value -> {
                try {
                    this.textSetting.valueFromString(value);
                    this.save.run();
                } catch (IllegalArgumentException ignored) {
                    // Keep the last valid value while an incomplete structured value is being typed.
                }
            });
            this.fixedSize(new Size(-1, TEXT_ROW_HEIGHT));
            this.addChild(this.field);
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SURFACE);
            renderer.line(0, bounds.height() - 1, bounds.width(), bounds.height() - 1, 1, BORDER_SOFT);
            float fieldWidth = this.fieldWidth(bounds.width());
            float labelWidth = Math.max(0F, bounds.width() - fieldWidth - 30F);
            drawFittedText(this, renderer, this.setting.name(), MUTED, 12, bounds.height() / 2F,
                    labelWidth, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            super.render(renderer, bounds);
        }

        @Override
        public void computeLayout(final Size size) {
            float fieldWidth = this.fieldWidth(size.width());
            this.field.layoutOptions(new AbsoluteLayoutOptions(size.width() - fieldWidth - 10F, 7F, fieldWidth, 24F));
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), TEXT_ROW_HEIGHT);
        }

        private float fieldWidth(final float rowWidth) {
            return Math.max(0F, Math.min(rowWidth - 28F, Math.max(86F, rowWidth * 0.48F)));
        }

        @Override
        public String layoutDebugLabel() {
            return this.setting.id();
        }
    }

    private final class FriendsPanel extends Container implements LayoutDebugLabel {

        private final TextField addField = textField("");
        private final Button addButton = button("Add", TEXT, ignored -> this.addFriend());
        private final Container rows = new Container(new VerticalListLayout(1, true));
        private final ScrollContainer scroll = new ScrollContainer(this.rows);

        private FriendsPanel() {
            super(AbsoluteLayout.INSTANCE);
            ModulePanel.this.configureScroll(this.scroll);
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
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SURFACE);
            drawText(this, renderer, "Friends", TEXT, 0, 12, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            super.render(renderer, bounds);
        }

        @Override
        public void computeLayout(final Size size) {
            this.addField.layoutOptions(new AbsoluteLayoutOptions(0F, 28F, Math.max(0F, size.width() - 70F), 28F));
            this.addButton.layoutOptions(new AbsoluteLayoutOptions(Math.max(0F, size.width() - 62F), 28F, 62F, 28F));
            this.scroll.layoutOptions(new AbsoluteLayoutOptions(0F, 66F, size.width(), Math.max(0F, size.height() - 66F)));
            super.computeLayout(size);
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

    private final class FriendRow extends Component implements LayoutDebugLabel {

        private final String friend;

        private FriendRow(final String friend) {
            this.friend = friend;
            this.fixedSize(new Size(-1, 34));
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SURFACE_SOFT);
            renderer.line(0, bounds.height() - 1, bounds.width(), bounds.height() - 1, 1, BORDER_SOFT);
            drawText(this, renderer, this.friend, TEXT, 12, bounds.height() / 2F, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            drawText(this, renderer, "Remove", MUTED, bounds.width() - 12, bounds.height() / 2F, TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() == MouseButton.LEFT && event.x() > bounds.width() - 76F) {
                if (AnarchyClient.FRIENDS.remove(this.friend)) {
                    ModulePanel.this.friendsPanel.refresh();
                }
                return true;
            }
            return false;
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

        private final TextField nameField = textField("");
        private final Button saveButton = button("Capture", TEXT, ignored -> this.capture());
        private final Container rows = new Container(new VerticalListLayout(1, true));
        private final ScrollContainer scroll = new ScrollContainer(this.rows);

        private ProfilesPanel() {
            super(AbsoluteLayout.INSTANCE);
            ModulePanel.this.configureScroll(this.scroll);
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
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SURFACE);
            drawText(this, renderer, "Profiles", TEXT, 0, 12, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            super.render(renderer, bounds);
        }

        @Override
        public void computeLayout(final Size size) {
            this.nameField.layoutOptions(new AbsoluteLayoutOptions(0F, 28F, Math.max(0F, size.width() - 92F), 28F));
            this.saveButton.layoutOptions(new AbsoluteLayoutOptions(Math.max(0F, size.width() - 84F), 28F, 84F, 28F));
            this.scroll.layoutOptions(new AbsoluteLayoutOptions(0F, 66F, size.width(), Math.max(0F, size.height() - 66F)));
            super.computeLayout(size);
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

    private final class ProfileRow extends Component implements LayoutDebugLabel {

        private final ProfileManager.ProfileSummary profile;

        private ProfileRow(final ProfileManager.ProfileSummary profile) {
            this.profile = profile;
            this.fixedSize(new Size(-1, 44));
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SURFACE_SOFT);
            renderer.line(0, bounds.height() - 1, bounds.width(), bounds.height() - 1, 1, BORDER_SOFT);
            drawText(this, renderer, this.profile.name(), TEXT, 12, bounds.height() / 2F - 7F, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            drawText(this, renderer, this.profile.modules() + " modules", FAINT, 12, bounds.height() / 2F + 7F, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            drawText(this, renderer, "Apply", ACTIVE, bounds.width() - 72, bounds.height() / 2F, TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            drawText(this, renderer, "Delete", MUTED, bounds.width() - 12, bounds.height() / 2F, TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() != MouseButton.LEFT) {
                return false;
            }
            if (event.x() > bounds.width() - 62F) {
                if (AnarchyClient.PROFILES.delete(this.profile.name())) {
                    ModulePanel.this.profilesPanel.refresh();
                }
                return true;
            }
            if (event.x() > bounds.width() - 124F) {
                AnarchyClient.PROFILES.apply(this.profile.name(), ModulePanel.this.modules);
                ModulePanel.this.config.save();
                ModulePanel.this.refreshModuleList();
                ModulePanel.this.inspector.refresh();
                return true;
            }
            return false;
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

    private final class SettingsDrawer extends Component implements LayoutDebugLabel {

        private void refresh() {
            ModulePanel.this.requestFrame();
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), Color.fromRGBA(13, 13, 15, 246));
            renderer.outlineRect(0, 0, bounds.width(), bounds.height(), 1, BORDER_SOFT);
            renderer.line(0, 38, bounds.width(), 38, 1, BORDER_SOFT);
            drawText(this, renderer, this.title(), TEXT, 30, 19, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            drawIcon(renderer, this.titleIcon(), 18F, 19F, MUTED);
            drawIcon(renderer, "x", bounds.width() - 18F, 19F, FAINT);
            switch (ModulePanel.this.drawer) {
                case ROOT -> this.renderRoot(renderer, bounds);
                case MODULES -> this.renderModules(renderer, bounds);
                case GUI -> this.renderGui(renderer, bounds);
                case NONE -> {
                }
            }
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() != MouseButton.LEFT) {
                return false;
            }
            if (event.x() >= bounds.width() - 28F && event.y() <= 38F) {
                ModulePanel.this.drawer = Drawer.NONE;
                ModulePanel.this.requestFrame();
                return true;
            }
            if (event.y() < 42F) {
                return false;
            }
            int index = (int) ((event.y() - 42F) / 36F);
            return switch (ModulePanel.this.drawer) {
                case ROOT -> this.clickRoot(index);
                case MODULES -> this.clickModules(index);
                case GUI -> this.clickGui(index);
                case NONE -> false;
            };
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
                case MODULES -> "Modules";
                case GUI -> "GUI";
                case NONE -> "";
            };
        }

        private String titleIcon() {
            return switch (ModulePanel.this.drawer) {
                case ROOT -> "settings";
                case MODULES -> "package";
                case GUI -> "layout";
                case NONE -> "circle";
            };
        }

        private void renderRoot(final Renderer renderer, final Rectangle bounds) {
            this.row(renderer, 0, "General", false, false, false);
            this.row(renderer, 1, "Modules", true, false, false);
            this.row(renderer, 2, "GUI", true, false, false);
            this.row(renderer, 3, "Sound", false, false, false);
            this.row(renderer, 4, "Notifications", false, false, false);
            this.row(renderer, 5, "GUI Theme", false, true, true);
            this.colorStrip(renderer, bounds, 42F + 5F * 36F + 29F);
        }

        private void renderModules(final Renderer renderer, final Rectangle bounds) {
            this.row(renderer, 0, "Show disabled modules", false, true, ModulePanel.this.showDisabledModules);
            this.row(renderer, 1, "Enabled modules first", false, true, ModulePanel.this.enabledFirst);
            this.row(renderer, 2, "Show module summaries", false, true, ModulePanel.this.showSummaries);
        }

        private void renderGui(final Renderer renderer, final Rectangle bounds) {
            this.row(renderer, 0, "Compact rows", false, true, ModulePanel.this.compactRows);
            this.row(renderer, 1, "Wide inspector", false, true, ModulePanel.this.wideInspector);
            this.row(renderer, 2, "GUI style - Central", false, false, false);
        }

        private void row(final Renderer renderer, final int index, final String text, final boolean arrow, final boolean toggle, final boolean checked) {
            float y = 42F + index * 36F;
            renderer.fillRect(0, y, DRAWER_WIDTH, 36F, SURFACE);
            renderer.line(0, y + 35F, DRAWER_WIDTH, y + 35F, 1, BORDER_SOFT);
            drawText(this, renderer, text, toggle || arrow || checked ? TEXT : MUTED, 12, y + 18F,
                    TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            if (toggle) {
                drawSwitch(renderer, DRAWER_WIDTH - 38F, y + 18F - SWITCH_HEIGHT / 2F, checked);
            } else if (arrow) {
                drawIcon(renderer, "chevron-right", DRAWER_WIDTH - 20F, y + 18F, FAINT);
            }
        }

        private void colorStrip(final Renderer renderer, final Rectangle bounds, final float y) {
            float x = 12F;
            float width = Math.max(0, bounds.width() - 24F);
            renderer.fillRect(x, y, width, 2F, TRACK);
            renderer.fillRect(x, y, width * 0.18F, 2F, Color.fromRGB(245, 56, 70));
            renderer.fillRect(x + width * 0.18F, y, width * 0.18F, 2F, Color.fromRGB(245, 132, 35));
            renderer.fillRect(x + width * 0.36F, y, width * 0.18F, 2F, Color.fromRGB(245, 204, 60));
            renderer.fillRect(x + width * 0.54F, y, width * 0.18F, 2F, ACTIVE);
            renderer.fillRect(x + width * 0.72F, y, width * 0.14F, 2F, Color.fromRGB(70, 116, 240));
            renderer.fillRect(x + width * 0.86F, y, width * 0.14F, 2F, Color.fromRGB(227, 70, 150));
        }

        private boolean clickRoot(final int index) {
            if (index == 1) {
                ModulePanel.this.drawer = Drawer.MODULES;
                ModulePanel.this.requestFrame();
                return true;
            }
            if (index == 2) {
                ModulePanel.this.drawer = Drawer.GUI;
                ModulePanel.this.requestFrame();
                return true;
            }
            return index >= 0 && index <= 5;
        }

        private boolean clickModules(final int index) {
            if (index == 0) {
                ModulePanel.this.showDisabledModules = !ModulePanel.this.showDisabledModules;
            } else if (index == 1) {
                ModulePanel.this.enabledFirst = !ModulePanel.this.enabledFirst;
            } else if (index == 2) {
                ModulePanel.this.showSummaries = !ModulePanel.this.showSummaries;
            } else {
                return false;
            }
            ModulePanel.this.refreshModuleList();
            ModulePanel.this.requestFrame();
            return true;
        }

        private boolean clickGui(final int index) {
            if (index == 0) {
                ModulePanel.this.compactRows = !ModulePanel.this.compactRows;
                ModulePanel.this.refreshModuleList();
            } else if (index == 1) {
                ModulePanel.this.wideInspector = !ModulePanel.this.wideInspector;
            } else {
                return index == 2;
            }
            ModulePanel.this.requestFrame();
            return true;
        }
    }

    private static final class EmptyState extends Component implements LayoutDebugLabel {

        private final String message;

        private EmptyState(final String message) {
            this.message = message;
            this.fixedSize(new Size(-1, 72));
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SURFACE);
            renderer.outlineRect(0, 0, bounds.width(), bounds.height(), 1, BORDER_SOFT);
            drawText(this, renderer, this.message, FAINT, 12, bounds.height() / 2F, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), 72);
        }

        @Override
        public String layoutDebugLabel() {
            return this.message;
        }
    }
}
