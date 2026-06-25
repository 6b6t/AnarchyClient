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
import java.util.function.Supplier;

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
    private static final float TOP_TAB_START_X = 144F;
    private static final float TOP_TAB_PADDING_X = 8F;
    private static final float TOP_TAB_GAP = 18F;
    private static final float TOP_TAB_UNDERLINE_Y = 34F;
    private static final float CATEGORY_ROW_X = 10F;
    private static final float CATEGORY_ROW_HEIGHT = 34F;
    private static final float CATEGORY_ROW_GAP = 6F;
    private static final float CATEGORY_ICON_CENTER_X = 28F;
    private static final float CATEGORY_TEXT_X = 48F;
    private static final float CATEGORY_COUNT_X_FROM_RIGHT = 24F;
    private static final float SEARCH_ICON_CENTER_X = 13F;
    private static final float SWITCH_WIDTH = 24F;
    private static final float SWITCH_HEIGHT = 12F;
    private static final float ICON_BOX = 20F;
    // The Lucide TTF rasterizes as a 20px icon font in Minecraft. Keep the
    // component box at that size so Rivet's component scissor does not crop it,
    // and calibrate the font's y anchor once inside IconNode.
    private static final float ICON_TEXT_Y_FROM_CENTER = 2F;

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
    private final ScrollContainer moduleScroll = new LayoutDebugScrollContainer(this.moduleRows);
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
        renderer.custom((GuiGraphicsExtractor graphics) -> graphics.text(
                Minecraft.getInstance().font,
                component,
                Math.round(left),
                Math.round(top),
                argb(color),
                false
        ), new Rectangle(0F, 0F, ICON_BOX, ICON_BOX));
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

    private static Surface surface(final Color color) {
        return new Surface(() -> color);
    }

    private static Surface surface(final Supplier<Color> color) {
        return new Surface(color);
    }

    private static Surface horizontalRule(final Color color) {
        return surface(color);
    }

    private static final class TextNode extends Component implements LayoutDebugLabel {

        private final Supplier<String> text;
        private final Supplier<Color> color;
        private TextOrigin.Horizontal horizontal = TextOrigin.Horizontal.LOGICAL_LEFT;
        private TextOrigin.Vertical vertical = TextOrigin.Vertical.LOGICAL_CENTER;

        private TextNode(final Supplier<String> text, final Supplier<Color> color) {
            this.text = text;
            this.color = color;
            this.interactive(false);
        }

        private TextNode origin(final TextOrigin.Horizontal horizontal, final TextOrigin.Vertical vertical) {
            this.horizontal = horizontal;
            this.vertical = vertical;
            return this;
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            String value = this.text.get();
            if (value.isBlank()) {
                return;
            }
            Color currentColor = this.color.get();
            float x = switch (this.horizontal) {
                case LOGICAL_LEFT, VISUAL_LEFT -> 0F;
                case VISUAL_CENTER -> bounds.width() / 2F;
                case VISUAL_RIGHT -> bounds.width();
            };
            drawFittedText(this, renderer, value, currentColor, x, bounds.height() / 2F,
                    bounds.width(), this.horizontal, this.vertical);
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
            return new Size(Math.min(textWidth(this, value, this.color.get()), constraints.width()), this.rivet().backend().getTextHeight());
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
            this.fixedSize(new Size(ICON_BOX, ICON_BOX));
            this.interactive(false);
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            drawIcon(renderer, this.icon.get(), bounds.width() / 2F, bounds.height() / 2F, this.color.get());
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

    private static final class Surface extends Component {

        private final Supplier<Color> color;
        private Supplier<Color> outlineColor = () -> Color.TRANSPARENT;
        private float outlineWidth;
        private float cornerRadius;

        private Surface(final Supplier<Color> color) {
            this.color = color;
            this.interactive(false);
        }

        private Surface outline(final Color color, final float width) {
            this.outlineColor = () -> color;
            this.outlineWidth = width;
            return this;
        }

        private Surface cornerRadius(final float cornerRadius) {
            this.cornerRadius = cornerRadius;
            return this;
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            Color fill = this.color.get();
            if (fill.getAlpha() > 0) {
                renderer.optimizedFillRoundedRect(0, 0, bounds.width(), bounds.height(), this.cornerRadius, fill);
            }
            Color outline = this.outlineColor.get();
            if (outline.getAlpha() > 0 && this.outlineWidth > 0F) {
                renderer.optimizedOutlineRoundedRect(0, 0, bounds.width(), bounds.height(), this.cornerRadius, this.outlineWidth, outline);
            }
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return Size.EMPTY;
        }
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

    private final class TopBar extends Container {

        private static final float REFRESH_ICON_X_FROM_RIGHT = 50F;
        private static final float SETTINGS_ICON_X_FROM_RIGHT = 24F;

        private final Surface background = surface(SHELL);
        private final TextNode brand = textNode("ANARCHY", TEXT);
        private final TextNode client = textNode("client", ACTIVE);
        private final List<TopTab> tabs = List.of(
                new TopTab(Tab.MODULES),
                new TopTab(Tab.FRIENDS),
                new TopTab(Tab.PROFILES)
        );
        private final IconNode refreshIcon = iconNode("refresh-cw", () -> ModulePanel.this.drawer == Drawer.NONE ? FAINT : MUTED);
        private final IconNode settingsIcon = iconNode("settings", () -> ModulePanel.this.drawer == Drawer.NONE ? MUTED : ACTIVE);

        private TopBar() {
            super(AbsoluteLayout.INSTANCE);
            this.addChild(this.background);
            this.addChild(this.brand);
            this.addChild(this.client);
            for (TopTab tab : this.tabs) {
                this.addChild(tab);
            }
            this.addChild(this.refreshIcon);
            this.addChild(this.settingsIcon);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() != MouseButton.LEFT) {
                return false;
            }
            float x = event.x();
            if (super.onComponentMouseDown(event, bounds)) {
                return true;
            }
            if (this.iconHit(event, x, this.iconCenterX(bounds.width(), SETTINGS_ICON_X_FROM_RIGHT), bounds.height())) {
                ModulePanel.this.openDrawer(Drawer.ROOT);
                return true;
            }
            if (this.iconHit(event, x, this.iconCenterX(bounds.width(), REFRESH_ICON_X_FROM_RIGHT), bounds.height())) {
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

        @Override
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.brand.layoutOptions(new AbsoluteLayoutOptions(14F, 0F, textWidth(this, "ANARCHY", TEXT), size.height()));
            float clientX = 14F + textWidth(this, "ANARCHY", TEXT) + 6F;
            this.client.layoutOptions(new AbsoluteLayoutOptions(clientX, 0F, textWidth(this, "client", ACTIVE), size.height()));

            float tabX = TOP_TAB_START_X;
            for (TopTab tab : this.tabs) {
                float width = tab.tabWidth();
                tab.layoutOptions(new AbsoluteLayoutOptions(tabX, 0F, width, size.height()));
                tabX += width + TOP_TAB_GAP;
            }

            this.refreshIcon.layoutOptions(this.iconOptions(size, REFRESH_ICON_X_FROM_RIGHT));
            this.settingsIcon.layoutOptions(this.iconOptions(size, SETTINGS_ICON_X_FROM_RIGHT));
            super.computeLayout(size);
        }

        private AbsoluteLayoutOptions iconOptions(final Size size, final float xFromRight) {
            float centerX = this.iconCenterX(size.width(), xFromRight);
            return new AbsoluteLayoutOptions(centerX - ICON_BOX / 2F, size.height() / 2F - ICON_BOX / 2F, ICON_BOX, ICON_BOX);
        }

        private float iconCenterX(final float width, final float xFromRight) {
            return width - xFromRight;
        }
    }

    private final class TopTab extends Container implements LayoutDebugLabel {

        private final Tab tab;
        private final TextNode label;
        private final Surface underline;

        private TopTab(final Tab tab) {
            super(AbsoluteLayout.INSTANCE);
            this.tab = tab;
            this.label = textNode(tab.label, () -> ModulePanel.this.selectedTab == tab ? TEXT : FAINT);
            this.underline = surface(() -> ModulePanel.this.selectedTab == this.tab ? ACTIVE : Color.TRANSPARENT);
            this.addChild(this.label);
            this.addChild(this.underline);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() == MouseButton.LEFT) {
                ModulePanel.this.selectTab(this.tab);
                return true;
            }
            return false;
        }

        @Override
        public void computeLayout(final Size size) {
            float labelWidth = this.labelWidth();
            this.label.layoutOptions(new AbsoluteLayoutOptions(TOP_TAB_PADDING_X, 0F, labelWidth, size.height()));
            this.underline.layoutOptions(new AbsoluteLayoutOptions(TOP_TAB_PADDING_X, TOP_TAB_UNDERLINE_Y, labelWidth, 2F));
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(this.tabWidth(), TOP_BAR_HEIGHT);
        }

        private float tabWidth() {
            return this.labelWidth() + TOP_TAB_PADDING_X * 2F;
        }

        private float labelWidth() {
            return textWidth(this, this.tab.label, TEXT);
        }

        @Override
        public String layoutDebugLabel() {
            return this.tab.name().toLowerCase(Locale.ROOT);
        }
    }

    private final class CategoryRail extends Container {

        private final Surface background = surface(SHELL);
        private final TextNode title = textNode("Modules", TEXT);
        private final List<CategoryButton> buttons = new ArrayList<>();

        private CategoryRail() {
            super(AbsoluteLayout.INSTANCE);
            this.addChild(this.background);
            this.addChild(this.title);
            for (ModuleCategory category : ModuleCategory.values()) {
                CategoryButton button = new CategoryButton(category);
                this.buttons.add(button);
                this.addChild(button);
            }
        }

        @Override
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.title.layoutOptions(new AbsoluteLayoutOptions(22F, 0F, Math.max(0F, size.width() - 44F), 56F));
            float y = 56F;
            for (CategoryButton button : this.buttons) {
                button.layoutOptions(new AbsoluteLayoutOptions(CATEGORY_ROW_X, y,
                        Math.max(0F, size.width() - CATEGORY_ROW_X * 2F), CATEGORY_ROW_HEIGHT));
                y += CATEGORY_ROW_HEIGHT + CATEGORY_ROW_GAP;
            }
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return constraints;
        }

    }

    private final class CategoryButton extends Container implements LayoutDebugLabel {

        private final ModuleCategory category;
        private final Surface background;
        private final Surface stripe;
        private final IconNode icon;
        private final TextNode label;
        private final TextNode count;

        private CategoryButton(final ModuleCategory category) {
            super(AbsoluteLayout.INSTANCE);
            this.category = category;
            this.background = surface(() -> this.selected() ? SURFACE_ACTIVE : Color.TRANSPARENT);
            this.stripe = surface(() -> this.selected() ? ACTIVE : Color.TRANSPARENT);
            this.icon = iconNode(categoryIcon(category), () -> this.selected() ? ACTIVE : FAINT);
            this.label = textNode(category.displayName(), () -> this.selected() ? TEXT : MUTED);
            this.count = textNode(this::enabledText, () -> this.selected() ? ACTIVE : FAINT)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.fixedSize(new Size(-1, CATEGORY_ROW_HEIGHT));
            this.addChild(this.background);
            this.addChild(this.stripe);
            this.addChild(this.icon);
            this.addChild(this.label);
            this.addChild(this.count);
        }

        @Override
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.stripe.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, 3F, size.height()));
            this.icon.layoutOptions(new AbsoluteLayoutOptions(
                    CATEGORY_ICON_CENTER_X - CATEGORY_ROW_X - ICON_BOX / 2F,
                    (size.height() - ICON_BOX) / 2F,
                    ICON_BOX,
                    ICON_BOX
            ));
            float labelX = CATEGORY_TEXT_X - CATEGORY_ROW_X;
            this.label.layoutOptions(new AbsoluteLayoutOptions(labelX, 0F,
                    Math.max(0F, size.width() - labelX - 36F), size.height()));
            this.count.layoutOptions(new AbsoluteLayoutOptions(0F, 0F,
                    Math.max(0F, size.width() - CATEGORY_COUNT_X_FROM_RIGHT + CATEGORY_ROW_X), size.height()));
            super.computeLayout(size);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() == MouseButton.LEFT) {
                ModulePanel.this.selectCategory(this.category);
                return true;
            }
            return false;
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), CATEGORY_ROW_HEIGHT);
        }

        @Override
        public String layoutDebugLabel() {
            return this.category.name().toLowerCase(Locale.ROOT);
        }

        private boolean selected() {
            return ModulePanel.this.selectedCategory == this.category;
        }

        private String enabledText() {
            int count = ModulePanel.this.enabledCount(this.category);
            return count > 0 ? String.valueOf(count) : "";
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
        private final Surface background = surface(FIELD).outline(BORDER, 1F).cornerRadius(CORNER_RADIUS);
        private final IconNode icon = iconNode("search", FAINT);
        private final TextNode placeholderLabel;
        private final TextField field = textField("");
        private Consumer<String> changeListener = ignored -> {
        };

        private SearchInput(final String placeholder) {
            super(AbsoluteLayout.INSTANCE);
            this.placeholder = placeholder;
            this.placeholderLabel = textNode(() -> this.field.text().isEmpty() ? this.placeholder : "", () -> FAINT);
            this.field.backgroundColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.outlineColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.focusedOutlineColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.valueChangeListener().add(value -> this.changeListener.accept(value));
            this.addChild(this.background);
            this.addChild(this.icon);
            this.addChild(this.placeholderLabel);
            this.addChild(this.field);
        }

        private void onChange(final Consumer<String> listener) {
            this.changeListener = listener;
        }

        @Override
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.icon.layoutOptions(new AbsoluteLayoutOptions(
                    SEARCH_ICON_CENTER_X - ICON_BOX / 2F,
                    (size.height() - ICON_BOX) / 2F,
                    ICON_BOX,
                    ICON_BOX
            ));
            this.placeholderLabel.layoutOptions(new AbsoluteLayoutOptions(26F, 0F, Math.max(0F, size.width() - 30F), size.height()));
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
        private static final float TEXT_SLOT_HEIGHT = 18F;

        private final Module module;
        private final Surface background;
        private final Surface stripe;
        private final TextNode name;
        private final TextNode summary;
        private final ToggleSwitch toggle;
        private final IconNode menuIcon = iconNode("more-vertical", FAINT);
        private boolean hovered;

        private ModuleRow(final Module module) {
            super(AbsoluteLayout.INSTANCE);
            this.module = module;
            this.background = surface(this::backgroundColor);
            this.stripe = surface(this::stripeColor);
            this.name = textNode(module.name(), () -> this.module.enabled() ? TEXT : MUTED);
            this.summary = textNode(() -> this.showSummary() ? moduleSummary(this.module) : "", () -> FAINT);
            this.toggle = new ToggleSwitch(module::enabled, () -> ModulePanel.this.toggleModule(module));
            this.fixedSize(new Size(-1, ModulePanel.this.compactRows ? COMPACT_MODULE_ROW_HEIGHT : MODULE_ROW_HEIGHT));
            this.addChild(this.background);
            this.addChild(this.stripe);
            this.addChild(this.name);
            this.addChild(this.summary);
            this.addChild(this.toggle);
            this.addChild(this.menuIcon);
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
        public void computeLayout(final Size size) {
            float controlsX = size.width() - 66F;
            float summaryX = 152F;
            boolean twoLine = this.showSummary();
            float nameMaxWidth = twoLine ? summaryX - 28F : controlsX - 28F;
            float nameCenterY = twoLine ? size.height() / 2F - 7F : size.height() / 2F;
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.stripe.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, 3F, size.height()));
            this.name.layoutOptions(new AbsoluteLayoutOptions(18F, nameCenterY - TEXT_SLOT_HEIGHT / 2F,
                    Math.max(0F, nameMaxWidth), TEXT_SLOT_HEIGHT));
            this.summary.layoutOptions(new AbsoluteLayoutOptions(summaryX, size.height() / 2F + 7F - TEXT_SLOT_HEIGHT / 2F,
                    Math.max(0F, controlsX - summaryX - 16F), TEXT_SLOT_HEIGHT));
            this.toggle.layoutOptions(new AbsoluteLayoutOptions(
                    size.width() - SWITCH_X_FROM_RIGHT, (size.height() - SWITCH_HEIGHT) / 2F, SWITCH_WIDTH, SWITCH_HEIGHT));
            this.menuIcon.layoutOptions(new AbsoluteLayoutOptions(
                    size.width() - KEBAB_X_FROM_RIGHT - ICON_BOX / 2F,
                    (size.height() - ICON_BOX) / 2F,
                    ICON_BOX,
                    ICON_BOX
            ));
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

        private boolean selected() {
            return this.module == ModulePanel.this.selectedModule;
        }

        private boolean showSummary() {
            return ModulePanel.this.showSummaries && !ModulePanel.this.compactRows;
        }

        private Color backgroundColor() {
            if (this.selected()) {
                return SURFACE_ACTIVE;
            }
            return this.hovered ? SURFACE_HOVER : SURFACE;
        }

        private Color stripeColor() {
            if (this.selected()) {
                return ACTIVE;
            }
            return this.module.enabled() ? ACTIVE.multiplyAlpha(0.55F) : Color.TRANSPARENT;
        }
    }

    private final class ModuleInspector extends Container implements LayoutDebugLabel {

        private final Surface background = surface(SURFACE);
        private final InspectorHeader header = new InspectorHeader();
        private final Container settings = new Container(new VerticalListLayout(0, true));
        private final ScrollContainer scroll = new LayoutDebugScrollContainer(this.settings);

        private ModuleInspector() {
            super(AbsoluteLayout.INSTANCE);
            ModulePanel.this.configureScroll(this.scroll);
            this.addChild(this.background);
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
        public void computeLayout(final Size size) {
            float headerHeight = 86F;
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
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

        private final Surface background = surface(SURFACE);
        private final Surface divider = horizontalRule(BORDER_SOFT);
        private final TextNode emptyLabel = textNode(() -> ModulePanel.this.selectedModule == null ? "No module" : "", () -> MUTED);
        private final TextNode title = textNode(() -> ModulePanel.this.selectedModule == null ? "" : ModulePanel.this.selectedModule.name(), () -> TEXT);
        private final TextNode category = textNode(
                () -> ModulePanel.this.selectedModule == null ? "" : ModulePanel.this.selectedModule.category().displayName(),
                () -> FAINT
        );
        private final TextNode enabledLabel = textNode(this::enabledLabel, this::enabledLabelColor)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        private final IconNode star = iconNode(() -> ModulePanel.this.selectedModule == null ? "" : "star", this::starColor);
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
            this.addChild(this.background);
            this.addChild(this.divider);
            this.addChild(this.emptyLabel);
            this.addChild(this.title);
            this.addChild(this.category);
            this.addChild(this.enabledLabel);
            this.addChild(this.star);
            this.addChild(this.toggle);
        }

        @Override
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.divider.layoutOptions(new AbsoluteLayoutOptions(0F, Math.max(0F, size.height() - 1F), size.width(), 1F));
            this.emptyLabel.layoutOptions(new AbsoluteLayoutOptions(16F, 0F, Math.max(0F, size.width() - 32F), size.height()));
            this.title.layoutOptions(new AbsoluteLayoutOptions(16F, TITLE_CENTER_Y - 9F,
                    Math.max(0F, size.width() - 112F), 18F));
            this.category.layoutOptions(new AbsoluteLayoutOptions(16F, 52F - 9F,
                    Math.max(0F, size.width() - 82F), 18F));
            String onText = this.enabledLabel();
            Color onColor = this.enabledLabelColor();
            float onX = size.width() - ON_LABEL_X_FROM_RIGHT;
            float onWidth = Math.max(26F, textWidth(this, onText, onColor));
            this.enabledLabel.layoutOptions(new AbsoluteLayoutOptions(onX - onWidth, TITLE_CENTER_Y - 9F, onWidth, 18F));
            float starX = onX - onWidth - 14F;
            this.star.layoutOptions(new AbsoluteLayoutOptions(starX - ICON_BOX / 2F, TITLE_CENTER_Y - ICON_BOX / 2F, ICON_BOX, ICON_BOX));
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

        private String enabledLabel() {
            Module module = ModulePanel.this.selectedModule;
            if (module == null) {
                return "";
            }
            return module.enabled() ? "ON" : "OFF";
        }

        private Color enabledLabelColor() {
            Module module = ModulePanel.this.selectedModule;
            return module != null && module.enabled() ? ACTIVE : FAINT;
        }

        private Color starColor() {
            Module module = ModulePanel.this.selectedModule;
            if (module == null) {
                return Color.TRANSPARENT;
            }
            return module.enabled() ? WARNING : FAINT;
        }
    }

    private static final class GroupHeader extends Container implements LayoutDebugLabel {

        private final String name;
        private final Surface background = surface(SHELL);
        private final TextNode label;

        private GroupHeader(final String name) {
            super(AbsoluteLayout.INSTANCE);
            this.name = name;
            this.fixedSize(new Size(-1, 24));
            this.label = textNode(name, MUTED);
            this.addChild(this.background);
            this.addChild(this.label);
        }

        @Override
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.label.layoutOptions(new AbsoluteLayoutOptions(12F, 0F, Math.max(0F, size.width() - 24F), size.height()));
            super.computeLayout(size);
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

        private final Setting<?> setting;
        private final float height;
        private final Surface background = surface(SURFACE);
        private final Surface divider = horizontalRule(BORDER_SOFT);
        private final TextNode label;

        private SettingLine(final Setting<?> setting, final float height) {
            super(AbsoluteLayout.INSTANCE);
            this.setting = setting;
            this.height = height;
            this.fixedSize(new Size(-1, height));
            this.label = textNode(setting.name(), MUTED);
            this.addChild(this.background);
            this.addChild(this.divider);
            this.addChild(this.label);
        }

        @Override
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.divider.layoutOptions(new AbsoluteLayoutOptions(0F, Math.max(0F, size.height() - 1F), size.width(), 1F));
            this.label.layoutOptions(new AbsoluteLayoutOptions(12F, 0F,
                    Math.max(0F, this.labelMaxWidth(size.width())), size.height()));
            this.layoutControls(size);
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), this.height);
        }

        protected void layoutControls(final Size size) {
        }

        protected float labelMaxWidth(final float width) {
            return width - 64F;
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

        private BooleanSettingRow(final BooleanSetting setting, final Runnable onChange) {
            super(setting, SETTING_ROW_HEIGHT);
            this.setting = setting;
            this.onChange = onChange;
            this.toggle = new ToggleSwitch(this.setting::value, this::toggle);
            this.addChild(this.toggle);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (super.onComponentMouseDown(event, bounds)) {
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
            this.toggle.layoutOptions(new AbsoluteLayoutOptions(
                    size.width() - 38F,
                    (size.height() - SWITCH_HEIGHT) / 2F,
                    SWITCH_WIDTH,
                    SWITCH_HEIGHT
            ));
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

        private SelectSettingRow(final SelectSetting setting, final Runnable onChange) {
            super(setting, SETTING_ROW_HEIGHT);
            this.setting = setting;
            this.onChange = onChange;
            this.value = textNode(this.setting::value, () -> TEXT)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.addChild(this.value);
            this.addChild(this.icon);
        }

        @Override
        protected float labelMaxWidth(final float width) {
            return width - Math.min(130F, Math.max(80F, width * 0.46F));
        }

        @Override
        protected void layoutControls(final Size size) {
            float valueWidth = Math.min(100F, Math.max(0F, size.width() * 0.42F));
            this.value.layoutOptions(new AbsoluteLayoutOptions(size.width() - 28F - valueWidth, 0F, valueWidth, size.height()));
            this.icon.layoutOptions(new AbsoluteLayoutOptions(
                    size.width() - 20F - ICON_BOX / 2F,
                    (size.height() - ICON_BOX) / 2F,
                    ICON_BOX,
                    ICON_BOX
            ));
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
        private final TextNode value;

        private ValueSettingRow(final Setting<?> setting) {
            super(setting, SETTING_ROW_HEIGHT);
            this.setting = setting;
            this.value = textNode(() -> SettingControls.displayValue(this.setting), () -> TEXT)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.addChild(this.value);
        }

        @Override
        protected void layoutControls(final Size size) {
            float valueWidth = Math.min(112F, Math.max(0F, size.width() * 0.44F));
            this.value.layoutOptions(new AbsoluteLayoutOptions(size.width() - 12F - valueWidth, 0F, valueWidth, size.height()));
        }

        @Override
        protected float labelMaxWidth(final float width) {
            return width - Math.min(136F, Math.max(84F, width * 0.48F));
        }
    }

    private static final class NumberSettingRow extends Container implements LayoutDebugLabel {

        private final NumberSetting setting;
        private final Surface background = surface(SURFACE);
        private final Surface divider = horizontalRule(BORDER_SOFT);
        private final TextNode label;
        private final TextNode value;
        private final Slider slider;
        private final Runnable save;

        private NumberSettingRow(final NumberSetting setting, final Runnable save) {
            super(AbsoluteLayout.INSTANCE);
            this.setting = setting;
            this.save = save;
            this.label = textNode(setting.name(), MUTED);
            this.value = textNode(() -> SettingControls.displayValue(this.setting), () -> TEXT)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.slider = slider(setting);
            this.slider.valueChangeListener().add(value -> {
                setting.value(value);
                this.save.run();
            });
            this.fixedSize(new Size(-1, NUMBER_ROW_HEIGHT));
            this.addChild(this.background);
            this.addChild(this.divider);
            this.addChild(this.label);
            this.addChild(this.value);
            this.addChild(this.slider);
        }

        @Override
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.divider.layoutOptions(new AbsoluteLayoutOptions(0F, Math.max(0F, size.height() - 1F), size.width(), 1F));
            float valueWidth = Math.min(58F, Math.max(0F, size.width() * 0.25F));
            this.label.layoutOptions(new AbsoluteLayoutOptions(12F, 6F, Math.max(0F, size.width() - 74F), 18F));
            this.value.layoutOptions(new AbsoluteLayoutOptions(size.width() - 12F - valueWidth, 6F, valueWidth, 18F));
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
        private final Surface background = surface(SURFACE);
        private final Surface divider = horizontalRule(BORDER_SOFT);
        private final TextNode label;
        private final TextField field;
        private final Runnable save;

        private TextSettingRow(final Setting<?> setting, final TextValueSetting textSetting, final Runnable save) {
            super(AbsoluteLayout.INSTANCE);
            this.setting = setting;
            this.textSetting = textSetting;
            this.save = save;
            this.label = textNode(setting.name(), MUTED);
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
            this.addChild(this.background);
            this.addChild(this.divider);
            this.addChild(this.label);
            this.addChild(this.field);
        }

        @Override
        public void computeLayout(final Size size) {
            float fieldWidth = this.fieldWidth(size.width());
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.divider.layoutOptions(new AbsoluteLayoutOptions(0F, Math.max(0F, size.height() - 1F), size.width(), 1F));
            this.label.layoutOptions(new AbsoluteLayoutOptions(12F, 0F,
                    Math.max(0F, size.width() - fieldWidth - 30F), size.height()));
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

        private final Surface background = surface(SURFACE);
        private final TextNode title = textNode("Friends", TEXT);
        private final TextField addField = textField("");
        private final Button addButton = button("Add", TEXT, ignored -> this.addFriend());
        private final Container rows = new Container(new VerticalListLayout(1, true));
        private final ScrollContainer scroll = new LayoutDebugScrollContainer(this.rows);

        private FriendsPanel() {
            super(AbsoluteLayout.INSTANCE);
            ModulePanel.this.configureScroll(this.scroll);
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
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.title.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), 24F));
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

    private final class FriendRow extends Container implements LayoutDebugLabel {

        private final String friend;
        private final Surface background = surface(SURFACE_SOFT);
        private final Surface divider = horizontalRule(BORDER_SOFT);
        private final TextNode name;
        private final TextNode remove = textNode("Remove", MUTED)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);

        private FriendRow(final String friend) {
            super(AbsoluteLayout.INSTANCE);
            this.friend = friend;
            this.fixedSize(new Size(-1, 34));
            this.name = textNode(friend, TEXT);
            this.addChild(this.background);
            this.addChild(this.divider);
            this.addChild(this.name);
            this.addChild(this.remove);
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
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.divider.layoutOptions(new AbsoluteLayoutOptions(0F, Math.max(0F, size.height() - 1F), size.width(), 1F));
            this.name.layoutOptions(new AbsoluteLayoutOptions(12F, 0F, Math.max(0F, size.width() - 96F), size.height()));
            this.remove.layoutOptions(new AbsoluteLayoutOptions(Math.max(0F, size.width() - 88F), 0F, 76F, size.height()));
            super.computeLayout(size);
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

        private final Surface background = surface(SURFACE);
        private final TextNode title = textNode("Profiles", TEXT);
        private final TextField nameField = textField("");
        private final Button saveButton = button("Capture", TEXT, ignored -> this.capture());
        private final Container rows = new Container(new VerticalListLayout(1, true));
        private final ScrollContainer scroll = new LayoutDebugScrollContainer(this.rows);

        private ProfilesPanel() {
            super(AbsoluteLayout.INSTANCE);
            ModulePanel.this.configureScroll(this.scroll);
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
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.title.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), 24F));
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

    private final class ProfileRow extends Container implements LayoutDebugLabel {

        private final ProfileManager.ProfileSummary profile;
        private final Surface background = surface(SURFACE_SOFT);
        private final Surface divider = horizontalRule(BORDER_SOFT);
        private final TextNode name;
        private final TextNode modules;
        private final TextNode apply = textNode("Apply", ACTIVE)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        private final TextNode delete = textNode("Delete", MUTED)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);

        private ProfileRow(final ProfileManager.ProfileSummary profile) {
            super(AbsoluteLayout.INSTANCE);
            this.profile = profile;
            this.fixedSize(new Size(-1, 44));
            this.name = textNode(profile.name(), TEXT);
            this.modules = textNode(profile.modules() + " modules", FAINT);
            this.addChild(this.background);
            this.addChild(this.divider);
            this.addChild(this.name);
            this.addChild(this.modules);
            this.addChild(this.apply);
            this.addChild(this.delete);
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
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.divider.layoutOptions(new AbsoluteLayoutOptions(0F, Math.max(0F, size.height() - 1F), size.width(), 1F));
            this.name.layoutOptions(new AbsoluteLayoutOptions(12F, size.height() / 2F - 16F, Math.max(0F, size.width() - 142F), 18F));
            this.modules.layoutOptions(new AbsoluteLayoutOptions(12F, size.height() / 2F - 2F, Math.max(0F, size.width() - 142F), 18F));
            this.apply.layoutOptions(new AbsoluteLayoutOptions(Math.max(0F, size.width() - 124F), 0F, 52F, size.height()));
            this.delete.layoutOptions(new AbsoluteLayoutOptions(Math.max(0F, size.width() - 62F), 0F, 50F, size.height()));
            super.computeLayout(size);
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

    private final class SettingsDrawer extends Container implements LayoutDebugLabel {

        private static final float HEADER_HEIGHT = 38F;
        private static final float ROW_START_Y = 42F;
        private static final float ROW_HEIGHT = 36F;

        private final Surface background = surface(Color.fromRGBA(13, 13, 15, 246)).outline(BORDER_SOFT, 1F);
        private final Surface headerDivider = horizontalRule(BORDER_SOFT);
        private final IconNode titleIcon = iconNode(this::titleIcon, () -> MUTED);
        private final TextNode title = textNode(this::title, () -> TEXT);
        private final IconNode closeIcon = iconNode("x", FAINT);
        private final List<DrawerChild> bodyChildren = new ArrayList<>();

        private SettingsDrawer() {
            super(AbsoluteLayout.INSTANCE);
            this.addChild(this.background);
            this.addChild(this.headerDivider);
            this.addChild(this.titleIcon);
            this.addChild(this.title);
            this.addChild(this.closeIcon);
        }

        private void refresh() {
            for (DrawerChild child : this.bodyChildren) {
                this.removeChild(child.component());
            }
            this.bodyChildren.clear();
            switch (ModulePanel.this.drawer) {
                case ROOT -> this.buildRoot();
                case MODULES -> this.buildModules();
                case GUI -> this.buildGui();
                case NONE -> {
                }
            }
            ModulePanel.this.requestFrame();
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
            return super.onComponentMouseDown(event, bounds);
        }

        @Override
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.headerDivider.layoutOptions(new AbsoluteLayoutOptions(0F, HEADER_HEIGHT, size.width(), 1F));
            float iconY = (HEADER_HEIGHT - ICON_BOX) / 2F;
            float titleX = 10F + ICON_BOX + 4F;
            this.titleIcon.layoutOptions(new AbsoluteLayoutOptions(10F, iconY, ICON_BOX, ICON_BOX));
            this.title.layoutOptions(new AbsoluteLayoutOptions(titleX, 0F, Math.max(0F, size.width() - titleX - 38F), HEADER_HEIGHT));
            this.closeIcon.layoutOptions(new AbsoluteLayoutOptions(size.width() - 10F - ICON_BOX, iconY, ICON_BOX, ICON_BOX));
            for (DrawerChild child : this.bodyChildren) {
                child.component().layoutOptions(new AbsoluteLayoutOptions(0F, child.y(), size.width(), child.height()));
            }
            super.computeLayout(size);
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

        private void buildRoot() {
            this.addRow(0, new DrawerRow("General", false, null, () -> {
            }));
            this.addRow(1, new DrawerRow("Modules", true, null, () -> this.openNestedDrawer(Drawer.MODULES)));
            this.addRow(2, new DrawerRow("GUI", true, null, () -> this.openNestedDrawer(Drawer.GUI)));
            this.addRow(3, new DrawerRow("Sound", false, null, () -> {
            }));
            this.addRow(4, new DrawerRow("Notifications", false, null, () -> {
            }));
            this.addRow(5, new DrawerRow("GUI Theme", false, () -> true, () -> {
            }));
            this.addBody(new ColorStrip(), ROW_START_Y + 5F * ROW_HEIGHT + 29F, 2F);
        }

        private void buildModules() {
            this.addRow(0, new DrawerRow("Show disabled modules", false,
                    () -> ModulePanel.this.showDisabledModules,
                    () -> {
                        ModulePanel.this.showDisabledModules = !ModulePanel.this.showDisabledModules;
                        ModulePanel.this.refreshModuleList();
                    }));
            this.addRow(1, new DrawerRow("Enabled modules first", false,
                    () -> ModulePanel.this.enabledFirst,
                    () -> {
                        ModulePanel.this.enabledFirst = !ModulePanel.this.enabledFirst;
                        ModulePanel.this.refreshModuleList();
                    }));
            this.addRow(2, new DrawerRow("Show module summaries", false,
                    () -> ModulePanel.this.showSummaries,
                    () -> {
                        ModulePanel.this.showSummaries = !ModulePanel.this.showSummaries;
                        ModulePanel.this.refreshModuleList();
                    }));
        }

        private void buildGui() {
            this.addRow(0, new DrawerRow("Compact rows", false,
                    () -> ModulePanel.this.compactRows,
                    () -> {
                        ModulePanel.this.compactRows = !ModulePanel.this.compactRows;
                        ModulePanel.this.refreshModuleList();
                    }));
            this.addRow(1, new DrawerRow("Wide inspector", false,
                    () -> ModulePanel.this.wideInspector,
                    () -> ModulePanel.this.wideInspector = !ModulePanel.this.wideInspector));
            this.addRow(2, new DrawerRow("GUI style - Central", false, null, () -> {
            }));
        }

        private void addRow(final int index, final DrawerRow row) {
            this.addBody(row, ROW_START_Y + index * ROW_HEIGHT, ROW_HEIGHT);
        }

        private void addBody(final Component component, final float y, final float height) {
            this.bodyChildren.add(new DrawerChild(component, y, height));
            this.addChild(component);
        }

        private void openNestedDrawer(final Drawer drawer) {
            ModulePanel.this.drawer = drawer;
            this.refresh();
        }

        private record DrawerChild(Component component, float y, float height) {
        }
    }

    private final class DrawerRow extends Container implements LayoutDebugLabel {

        private final String text;
        private final BooleanSupplier checked;
        private final Runnable action;
        private final Surface background = surface(SURFACE);
        private final Surface divider = horizontalRule(BORDER_SOFT);
        private final TextNode label;
        private final IconNode arrow;
        private final ToggleSwitch toggle;

        private DrawerRow(final String text, final boolean arrow, final BooleanSupplier checked, final Runnable action) {
            super(AbsoluteLayout.INSTANCE);
            this.text = text;
            this.checked = checked;
            this.action = action;
            this.label = textNode(text, () -> arrow || this.checked() ? TEXT : MUTED);
            this.arrow = arrow ? iconNode("chevron-right", FAINT) : null;
            this.toggle = checked == null ? null : new ToggleSwitch(this::checked, this::runAction);
            this.fixedSize(new Size(-1, SettingsDrawer.ROW_HEIGHT));
            this.addChild(this.background);
            this.addChild(this.divider);
            this.addChild(this.label);
            if (this.arrow != null) {
                this.addChild(this.arrow);
            }
            if (this.toggle != null) {
                this.addChild(this.toggle);
            }
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (super.onComponentMouseDown(event, bounds)) {
                return true;
            }
            if (event.button() == MouseButton.LEFT) {
                this.runAction();
                return true;
            }
            return false;
        }

        @Override
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.divider.layoutOptions(new AbsoluteLayoutOptions(0F, Math.max(0F, size.height() - 1F), size.width(), 1F));
            this.label.layoutOptions(new AbsoluteLayoutOptions(12F, 0F, Math.max(0F, size.width() - 58F), size.height()));
            if (this.toggle != null) {
                this.toggle.layoutOptions(new AbsoluteLayoutOptions(
                        size.width() - 38F,
                        (size.height() - SWITCH_HEIGHT) / 2F,
                        SWITCH_WIDTH,
                        SWITCH_HEIGHT
                ));
            }
            if (this.arrow != null) {
                this.arrow.layoutOptions(new AbsoluteLayoutOptions(
                        size.width() - 20F - ICON_BOX / 2F,
                        (size.height() - ICON_BOX) / 2F,
                        ICON_BOX,
                        ICON_BOX
                ));
            }
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), SettingsDrawer.ROW_HEIGHT);
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
            ModulePanel.this.settingsDrawer.refresh();
            ModulePanel.this.requestFrame();
        }
    }

    private static final class ColorStrip extends Component implements LayoutDebugLabel {

        private static final List<Color> COLORS = List.of(
                Color.fromRGB(245, 56, 70),
                Color.fromRGB(245, 132, 35),
                Color.fromRGB(245, 204, 60),
                ACTIVE,
                Color.fromRGB(70, 116, 240),
                Color.fromRGB(227, 70, 150)
        );
        private static final List<Float> FRACTIONS = List.of(0.18F, 0.18F, 0.18F, 0.18F, 0.14F, 0.14F);

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            float x = 12F;
            float width = Math.max(0F, bounds.width() - 24F);
            renderer.fillRect(x, 0F, width, bounds.height(), TRACK);
            float offset = 0F;
            for (int index = 0; index < COLORS.size(); index++) {
                float segmentWidth = width * FRACTIONS.get(index);
                renderer.fillRect(x + offset, 0F, segmentWidth, bounds.height(), COLORS.get(index));
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
    }

    private static final class EmptyState extends Container implements LayoutDebugLabel {

        private final String message;
        private final Surface background = surface(SURFACE).outline(BORDER_SOFT, 1F);
        private final TextNode label;

        private EmptyState(final String message) {
            super(AbsoluteLayout.INSTANCE);
            this.message = message;
            this.fixedSize(new Size(-1, 72));
            this.label = textNode(message, FAINT);
            this.addChild(this.background);
            this.addChild(this.label);
        }

        @Override
        public void computeLayout(final Size size) {
            this.background.layoutOptions(new AbsoluteLayoutOptions(0F, 0F, size.width(), size.height()));
            this.label.layoutOptions(new AbsoluteLayoutOptions(12F, 0F, Math.max(0F, size.width() - 24F), size.height()));
            super.computeLayout(size);
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
