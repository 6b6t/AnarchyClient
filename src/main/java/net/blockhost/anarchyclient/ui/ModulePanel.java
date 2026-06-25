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
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.layout.grid.GridLayout;
import net.lenni0451.rivet.layout.grid.GridLayoutOptions;
import net.lenni0451.rivet.layout.list.HorizontalListLayout;
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
    private static final float CATEGORY_ROW_X = 10F;
    private static final float CATEGORY_ROW_HEIGHT = 34F;
    private static final float CATEGORY_ROW_GAP = 6F;
    private static final float SWITCH_WIDTH = 24F;
    private static final float SWITCH_HEIGHT = 12F;
    private static final float ICON_BOX = 20F;
    private static final float ICON_BUTTON_SIZE = 28F;
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

    private static GridLayoutOptions fillCell(final int column, final int row) {
        return cell(column, row, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, null);
    }

    private static GridLayoutOptions fillCell(final int column, final int row, final int columnSpan, final int rowSpan) {
        return cell(column, row, columnSpan, rowSpan, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, null);
    }

    private static GridLayoutOptions weightedCell(final int column, final int row, final float weightX, final float weightY) {
        return cell(column, row, 1, 1, weightX, weightY, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, null);
    }

    private static GridLayoutOptions weightedCell(final int column, final int row, final int columnSpan, final int rowSpan,
                                                  final float weightX, final float weightY) {
        return cell(column, row, columnSpan, rowSpan, weightX, weightY, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, null);
    }

    private static GridLayoutOptions fixedCell(final int column, final int row, final float width, final float height,
                                               final GridAnchor anchor) {
        return cell(column, row, 1, 1, 0F, 0F, anchor, GridFill.NONE, Padding.EMPTY, width, height);
    }

    private static GridLayoutOptions fixedCell(final int column, final int row, final int columnSpan, final int rowSpan,
                                               final float width, final float height, final GridAnchor anchor) {
        return cell(column, row, columnSpan, rowSpan, 0F, 0F, anchor, GridFill.NONE, Padding.EMPTY, width, height);
    }

    private static GridLayoutOptions cell(final int column, final int row, final int columnSpan, final int rowSpan,
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
        return new GridLayoutOptions(column, row, columnSpan, rowSpan, weightX, weightY, anchor, fill, padding, effectiveWidth, effectiveHeight);
    }

    private static Padding padding(final float left, final float top, final float right, final float bottom) {
        return new Padding(left, top, right, bottom);
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

    private static IconButton iconButton(final String icon, final Supplier<Color> color, final Runnable action) {
        return new IconButton(() -> icon, color, action);
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
            float iconX = (bounds.width() - ICON_BOX) / 2F;
            float iconY = (bounds.height() - ICON_BOX) / 2F;
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

        private IconButton(final Supplier<String> icon, final Supplier<Color> color, final Runnable action) {
            this.icon = icon;
            this.color = color;
            this.action = action;
            this.fixedSize(new Size(ICON_BUTTON_SIZE, ICON_BUTTON_SIZE));
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            drawIcon(renderer, this.icon.get(), bounds.width() / 2F, bounds.height() / 2F, this.color.get());
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() != MouseButton.LEFT) {
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
            return this.icon.get();
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
        public void render(final Renderer renderer, final Rectangle bounds) {
            String value = this.text.get();
            if (value.isBlank()) {
                return;
            }
            drawFittedText(this, renderer, value, this.color.get(), bounds.width(), bounds.height() / 2F,
                    bounds.width(), TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
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
            return new Size(Math.min(textWidth(this, value, this.color.get()), constraints.width()), this.rivet().backend().getTextHeight());
        }

        @Override
        public String layoutDebugLabel() {
            return this.text.get();
        }
    }

    private static final class BrandBlock extends Component {

        private BrandBlock() {
            this.interactive(false);
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            float brandWidth = textWidth(this, "ANARCHY", TEXT);
            drawFittedText(this, renderer, "ANARCHY", TEXT, 14F, bounds.height() / 2F,
                    Math.max(0F, bounds.width() - 44F), TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
            drawFittedText(this, renderer, "client", ACTIVE, 14F + brandWidth + 6F, bounds.height() / 2F,
                    Math.max(0F, bounds.width() - brandWidth - 20F), TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.LOGICAL_CENTER);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(TOP_TAB_START_X, TOP_BAR_HEIGHT);
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

        private final BooleanSupplier visible;
        private final BooleanSupplier state;
        private final Runnable onToggle;

        private ToggleSwitch(final BooleanSupplier state, final Runnable onToggle) {
            this(() -> true, state, onToggle);
        }

        private ToggleSwitch(final BooleanSupplier visible, final BooleanSupplier state, final Runnable onToggle) {
            this.visible = visible;
            this.state = state;
            this.onToggle = onToggle;
            this.fixedSize(new Size(SWITCH_WIDTH, SWITCH_HEIGHT));
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            if (!this.visible.getAsBoolean()) {
                return;
            }
            drawSwitch(renderer, 0, 0, this.state.getAsBoolean());
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
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

        private final Surface background = surface(SHELL);
        private final BrandBlock brand = new BrandBlock();
        private final Container tabStrip = new Container(new HorizontalListLayout((int) TOP_TAB_GAP, true));
        private final List<TopTab> tabs = List.of(
                new TopTab(Tab.MODULES),
                new TopTab(Tab.FRIENDS),
                new TopTab(Tab.PROFILES)
        );
        private final IconButton refreshIcon = iconButton("refresh-cw", () -> ModulePanel.this.drawer == Drawer.NONE ? FAINT : MUTED, () -> {
            ModulePanel.this.refreshModuleList();
            ModulePanel.this.inspector.refresh();
        });
        private final IconButton settingsIcon = iconButton("settings", () -> ModulePanel.this.drawer == Drawer.NONE ? MUTED : ACTIVE,
                () -> ModulePanel.this.openDrawer(Drawer.ROOT));

        private TopBar() {
            super(new GridLayout(0, 0));
            this.background.layoutOptions(fillCell(0, 0, 4, 1));
            this.brand.layoutOptions(fixedCell(0, 0, TOP_TAB_START_X, TOP_BAR_HEIGHT, GridAnchor.CENTER));
            this.tabStrip.layoutOptions(weightedCell(1, 0, 1F, 0F));
            this.refreshIcon.layoutOptions(fixedCell(2, 0, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE, GridAnchor.CENTER));
            this.settingsIcon.layoutOptions(cell(3, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 10F, 0F), ICON_BUTTON_SIZE, ICON_BUTTON_SIZE));
            this.addChild(this.background);
            this.addChild(this.brand);
            for (TopTab tab : this.tabs) {
                this.tabStrip.addChild(tab);
            }
            this.addChild(this.tabStrip);
            this.addChild(this.refreshIcon);
            this.addChild(this.settingsIcon);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), TOP_BAR_HEIGHT);
        }
    }

    private final class TopTab extends Container implements LayoutDebugLabel {

        private final Tab tab;
        private final TextNode label;
        private final Surface underline;

        private TopTab(final Tab tab) {
            super(new GridLayout(0, 0));
            this.tab = tab;
            this.label = textNode(tab.label, () -> ModulePanel.this.selectedTab == tab ? TEXT : FAINT);
            this.underline = surface(() -> ModulePanel.this.selectedTab == this.tab ? ACTIVE : Color.TRANSPARENT);
            this.label.layoutOptions(cell(0, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(TOP_TAB_PADDING_X, 0F, TOP_TAB_PADDING_X, 0F), null, null));
            this.underline.layoutOptions(cell(0, 1, 1, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    padding(TOP_TAB_PADDING_X, 0F, TOP_TAB_PADDING_X, 0F), null, 2F));
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
            super(new GridLayout(0, (int) CATEGORY_ROW_GAP));
            int rows = ModuleCategory.values().length + 1;
            this.background.layoutOptions(fillCell(0, 0, 1, rows));
            this.title.layoutOptions(cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(22F, 0F, 22F, 0F), null, 56F));
            this.addChild(this.background);
            this.addChild(this.title);
            int row = 1;
            for (ModuleCategory category : ModuleCategory.values()) {
                CategoryButton button = new CategoryButton(category);
                button.layoutOptions(cell(0, row, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                        padding(CATEGORY_ROW_X, 0F, CATEGORY_ROW_X, 0F), null, CATEGORY_ROW_HEIGHT));
                this.buttons.add(button);
                this.addChild(button);
                row++;
            }
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
            super(new GridLayout(0, 0));
            this.category = category;
            this.background = surface(() -> this.selected() ? SURFACE_ACTIVE : Color.TRANSPARENT);
            this.stripe = surface(() -> this.selected() ? ACTIVE : Color.TRANSPARENT);
            this.icon = iconNode(categoryIcon(category), () -> this.selected() ? ACTIVE : FAINT);
            this.label = textNode(category.displayName(), () -> this.selected() ? TEXT : MUTED);
            this.count = textNode(this::enabledText, () -> this.selected() ? ACTIVE : FAINT)
                    .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
            this.fixedSize(new Size(-1, CATEGORY_ROW_HEIGHT));
            this.background.layoutOptions(fillCell(0, 0, 4, 1));
            this.stripe.layoutOptions(fixedCell(0, 0, 3F, CATEGORY_ROW_HEIGHT, GridAnchor.LEFT));
            this.icon.layoutOptions(cell(1, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.NONE,
                    padding(5F, 0F, 5F, 0F), ICON_BOX, ICON_BOX));
            this.label.layoutOptions(cell(2, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(4F, 0F, 8F, 0F), null, null));
            this.count.layoutOptions(cell(3, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 8F, 0F), 36F, null));
            this.addChild(this.background);
            this.addChild(this.stripe);
            this.addChild(this.icon);
            this.addChild(this.label);
            this.addChild(this.count);
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

        private static final float FIELD_RIGHT_PADDING = 4F;
        private static final float FIELD_VERTICAL_PADDING = 4F;

        private final String placeholder;
        private final Surface background = surface(FIELD).outline(BORDER, 1F).cornerRadius(CORNER_RADIUS);
        private final IconNode icon = iconNode("search", FAINT);
        private final TextNode placeholderLabel;
        private final TextField field = textField("");
        private Consumer<String> changeListener = ignored -> {
        };

        private SearchInput(final String placeholder) {
            super(new GridLayout(0, 0));
            this.placeholder = placeholder;
            this.placeholderLabel = textNode(() -> this.field.text().isEmpty() && !this.fieldFocused() ? this.placeholder : "", () -> FAINT);
            this.field.backgroundColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.outlineColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.focusedOutlineColor().set(Color.fromRGBA(0, 0, 0, 0));
            this.field.valueChangeListener().add(value -> this.changeListener.accept(value));
            this.background.layoutOptions(fillCell(0, 0, 2, 1));
            this.icon.layoutOptions(fixedCell(0, 0, ICON_BOX, ICON_BOX, GridAnchor.CENTER));
            this.placeholderLabel.layoutOptions(cell(1, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, FIELD_RIGHT_PADDING, 0F), null, null));
            this.field.layoutOptions(cell(1, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, FIELD_VERTICAL_PADDING / 2F, FIELD_RIGHT_PADDING, FIELD_VERTICAL_PADDING / 2F), null, null));
            this.addChild(this.background);
            this.addChild(this.icon);
            this.addChild(this.placeholderLabel);
            this.addChild(this.field);
        }

        private void onChange(final Consumer<String> listener) {
            this.changeListener = listener;
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

    private final class ModuleRow extends Container implements LayoutDebugLabel {

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
            super(new GridLayout(0, 0));
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
            boolean twoLine = this.showSummary();
            int rows = twoLine ? 2 : 1;
            this.background.layoutOptions(fillCell(0, 0, 5, rows));
            this.stripe.layoutOptions(fixedCell(0, 0, 1, rows, 3F, size.height(), GridAnchor.LEFT));
            this.toggle.layoutOptions(cell(3, 0, 1, rows, 0F, 0F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 2F, 0F), SWITCH_WIDTH, SWITCH_HEIGHT));
            this.menuIcon.layoutOptions(cell(4, 0, 1, rows, 0F, 0F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 12F, 0F), ICON_BOX, ICON_BOX));
            if (twoLine) {
                this.name.layoutOptions(cell(1, 0, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                        padding(15F, 0F, 12F, 0F), null, TEXT_SLOT_HEIGHT));
                this.summary.layoutOptions(cell(1, 1, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                        padding(15F, 0F, 12F, 0F), null, TEXT_SLOT_HEIGHT));
            } else {
                this.name.layoutOptions(cell(1, 0, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                        padding(15F, 0F, 12F, 0F), null, null));
                this.summary.layoutOptions(cell(1, 0, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                        padding(15F, 0F, 12F, 0F), null, null));
            }
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
            super(new GridLayout(0, 0));
            ModulePanel.this.configureScroll(this.scroll);
            this.background.layoutOptions(fillCell(0, 0, 1, 2));
            this.header.layoutOptions(cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH, Padding.EMPTY, null, 86F));
            this.scroll.layoutOptions(weightedCell(0, 1, 1F, 1F));
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
        public Size computeIdealSize(final Size constraints) {
            return constraints;
        }

        @Override
        public String layoutDebugLabel() {
            return ModulePanel.this.selectedModule == null ? "empty" : ModulePanel.this.selectedModule.id();
        }
    }

    private final class InspectorHeader extends Container implements LayoutDebugLabel {

        private final Surface background = surface(SURFACE);
        private final Surface divider = horizontalRule(BORDER_SOFT);
        private final TextNode title = textNode(() -> ModulePanel.this.selectedModule == null ? "No module" : ModulePanel.this.selectedModule.name(),
                () -> ModulePanel.this.selectedModule == null ? MUTED : TEXT);
        private final TextNode category = textNode(
                () -> ModulePanel.this.selectedModule == null ? "" : ModulePanel.this.selectedModule.category().displayName(),
                () -> FAINT
        );
        private final TextNode enabledLabel = textNode(this::enabledLabel, this::enabledLabelColor)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        private final IconNode star = iconNode(() -> ModulePanel.this.selectedModule == null ? "" : "star", this::starColor);
        private final ToggleSwitch toggle;

        private InspectorHeader() {
            super(new GridLayout(0, 0));
            this.toggle = new ToggleSwitch(
                    () -> ModulePanel.this.selectedModule != null,
                    () -> ModulePanel.this.selectedModule != null && ModulePanel.this.selectedModule.enabled(),
                    () -> {
                        if (ModulePanel.this.selectedModule != null) {
                            ModulePanel.this.toggleModule(ModulePanel.this.selectedModule);
                        }
                    });
            this.background.layoutOptions(fillCell(0, 0, 4, 3));
            this.divider.layoutOptions(cell(0, 2, 4, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    Padding.EMPTY, null, 1F));
            this.title.layoutOptions(cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(16F, 0F, 12F, 0F), null, 18F));
            this.category.layoutOptions(cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(16F, 0F, 12F, 0F), null, 18F));
            this.star.layoutOptions(fixedCell(1, 0, 1, 2, ICON_BOX, ICON_BOX, GridAnchor.CENTER));
            this.enabledLabel.layoutOptions(cell(2, 0, 1, 2, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 4F, 0F), 30F, null));
            this.toggle.layoutOptions(cell(3, 0, 1, 2, 0F, 0F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 16F, 0F), SWITCH_WIDTH, SWITCH_HEIGHT));
            this.addChild(this.background);
            this.addChild(this.divider);
            this.addChild(this.title);
            this.addChild(this.category);
            this.addChild(this.enabledLabel);
            this.addChild(this.star);
            this.addChild(this.toggle);
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
            super(new GridLayout(0, 0));
            this.name = name;
            this.fixedSize(new Size(-1, 24));
            this.label = textNode(name, MUTED);
            this.background.layoutOptions(fillCell(0, 0));
            this.label.layoutOptions(cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(12F, 0F, 12F, 0F), null, null));
            this.addChild(this.background);
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

        private final Setting<?> setting;
        private final float height;
        private final Surface background = surface(SURFACE);
        private final Surface divider = horizontalRule(BORDER_SOFT);
        private final TextNode label;

        private SettingLine(final Setting<?> setting, final float height) {
            super(new GridLayout(0, 0));
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
            this.background.layoutOptions(fillCell(0, 0, 4, 2));
            this.divider.layoutOptions(cell(0, 1, 4, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    Padding.EMPTY, null, 1F));
            this.label.layoutOptions(cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(12F, 0F, 8F, 0F), null, null));
            this.layoutControls(size);
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), this.height);
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
            this.toggle.layoutOptions(cell(3, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
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
        protected void layoutControls(final Size size) {
            float valueWidth = Math.min(100F, Math.max(0F, size.width() * 0.42F));
            this.value.layoutOptions(cell(2, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 2F, 0F), valueWidth, null));
            this.icon.layoutOptions(cell(3, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                    padding(0F, 0F, 8F, 0F), ICON_BOX, ICON_BOX));
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
            this.value.layoutOptions(cell(2, 0, 2, 1, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 12F, 0F), valueWidth, null));
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
            super(new GridLayout(0, 0));
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
            float valueWidth = Math.min(58F, Math.max(0F, size.width() * 0.25F));
            this.background.layoutOptions(fillCell(0, 0, 2, 3));
            this.divider.layoutOptions(cell(0, 2, 2, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    Padding.EMPTY, null, 1F));
            this.label.layoutOptions(cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(12F, 4F, 8F, 0F), null, 18F));
            this.value.layoutOptions(cell(1, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 4F, 12F, 0F), valueWidth, 18F));
            this.slider.layoutOptions(cell(0, 1, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.HORIZONTAL,
                    padding(12F, 0F, 12F, 0F), null, 16F));
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
            super(new GridLayout(0, 0));
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
            this.background.layoutOptions(fillCell(0, 0, 2, 2));
            this.divider.layoutOptions(cell(0, 1, 2, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    Padding.EMPTY, null, 1F));
            this.label.layoutOptions(cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(12F, 0F, 8F, 0F), null, null));
            this.field.layoutOptions(cell(1, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 7F, 10F, 7F), null, 24F));
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), TEXT_ROW_HEIGHT);
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
            super(new GridLayout(8, 4));
            ModulePanel.this.configureScroll(this.scroll);
            this.background.layoutOptions(fillCell(0, 0, 2, 3));
            this.title.layoutOptions(cell(0, 0, 2, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    Padding.EMPTY, null, 24F));
            this.addField.layoutOptions(weightedCell(0, 1, 1F, 0F));
            this.addButton.layoutOptions(cell(1, 1, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    Padding.EMPTY, 62F, 28F));
            this.scroll.layoutOptions(weightedCell(0, 2, 2, 1, 1F, 1F));
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

        private final String friend;
        private final Surface background = surface(SURFACE_SOFT);
        private final Surface divider = horizontalRule(BORDER_SOFT);
        private final TextNode name;
        private final TextAction remove;

        private FriendRow(final String friend) {
            super(new GridLayout(0, 0));
            this.friend = friend;
            this.fixedSize(new Size(-1, 34));
            this.name = textNode(friend, TEXT);
            this.remove = textAction("Remove", MUTED, () -> {
                if (AnarchyClient.FRIENDS.remove(this.friend)) {
                    ModulePanel.this.friendsPanel.refresh();
                }
            });
            this.background.layoutOptions(fillCell(0, 0, 2, 2));
            this.divider.layoutOptions(cell(0, 1, 2, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    Padding.EMPTY, null, 1F));
            this.name.layoutOptions(cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(12F, 0F, 8F, 0F), null, null));
            this.remove.layoutOptions(cell(1, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 12F, 0F), 76F, null));
            this.addChild(this.background);
            this.addChild(this.divider);
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

        private final Surface background = surface(SURFACE);
        private final TextNode title = textNode("Profiles", TEXT);
        private final TextField nameField = textField("");
        private final Button saveButton = button("Capture", TEXT, ignored -> this.capture());
        private final Container rows = new Container(new VerticalListLayout(1, true));
        private final ScrollContainer scroll = new LayoutDebugScrollContainer(this.rows);

        private ProfilesPanel() {
            super(new GridLayout(8, 4));
            ModulePanel.this.configureScroll(this.scroll);
            this.background.layoutOptions(fillCell(0, 0, 2, 3));
            this.title.layoutOptions(cell(0, 0, 2, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    Padding.EMPTY, null, 24F));
            this.nameField.layoutOptions(weightedCell(0, 1, 1F, 0F));
            this.saveButton.layoutOptions(cell(1, 1, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    Padding.EMPTY, 84F, 28F));
            this.scroll.layoutOptions(weightedCell(0, 2, 2, 1, 1F, 1F));
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
        private final Surface background = surface(SURFACE_SOFT);
        private final Surface divider = horizontalRule(BORDER_SOFT);
        private final TextNode name;
        private final TextNode modules;
        private final TextAction apply;
        private final TextAction delete;

        private ProfileRow(final ProfileManager.ProfileSummary profile) {
            super(new GridLayout(0, 0));
            this.profile = profile;
            this.fixedSize(new Size(-1, 44));
            this.name = textNode(profile.name(), TEXT);
            this.modules = textNode(profile.modules() + " modules", FAINT);
            this.apply = textAction("Apply", ACTIVE, () -> {
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
            this.background.layoutOptions(fillCell(0, 0, 3, 3));
            this.divider.layoutOptions(cell(0, 2, 3, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    Padding.EMPTY, null, 1F));
            this.name.layoutOptions(cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(12F, 0F, 8F, 0F), null, 18F));
            this.modules.layoutOptions(cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(12F, 0F, 8F, 0F), null, 18F));
            this.apply.layoutOptions(cell(1, 0, 1, 2, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 10F, 0F), 52F, null));
            this.delete.layoutOptions(cell(2, 0, 1, 2, 0F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(0F, 0F, 12F, 0F), 50F, null));
            this.addChild(this.background);
            this.addChild(this.divider);
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

    private final class SettingsDrawer extends Container implements LayoutDebugLabel {

        private static final float HEADER_HEIGHT = 38F;
        private static final float ROW_HEIGHT = 36F;

        private final Surface background = surface(Color.fromRGBA(13, 13, 15, 246)).outline(BORDER_SOFT, 1F);
        private final Surface headerDivider = horizontalRule(BORDER_SOFT);
        private final IconNode titleIcon = iconNode(this::titleIcon, () -> MUTED);
        private final TextNode title = textNode(this::title, () -> TEXT);
        private final IconButton closeIcon = iconButton("x", () -> FAINT, () -> {
            ModulePanel.this.drawer = Drawer.NONE;
            ModulePanel.this.requestFrame();
        });
        private final Container body = new Container(new VerticalListLayout(0, true));

        private SettingsDrawer() {
            super(new GridLayout(0, 4));
            this.background.layoutOptions(fillCell(0, 0, 1, 2));
            this.headerDivider.layoutOptions(cell(0, 0, 1, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    Padding.EMPTY, null, 1F));
            this.titleIcon.layoutOptions(cell(0, 0, 1, 1, 0F, 0F, GridAnchor.LEFT, GridFill.NONE,
                    padding(10F, 0F, 0F, 0F), ICON_BOX, ICON_BOX));
            this.title.layoutOptions(cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(34F, 0F, 38F, 0F), null, HEADER_HEIGHT));
            this.closeIcon.layoutOptions(cell(0, 0, 1, 1, 0F, 0F, GridAnchor.RIGHT, GridFill.NONE,
                    padding(0F, 0F, 6F, 0F), ICON_BUTTON_SIZE, ICON_BUTTON_SIZE));
            this.body.layoutOptions(weightedCell(0, 1, 1F, 1F));
            this.addChild(this.background);
            this.addChild(this.headerDivider);
            this.addChild(this.titleIcon);
            this.addChild(this.title);
            this.addChild(this.closeIcon);
            this.addChild(this.body);
        }

        private void refresh() {
            this.body.clearChildren();
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
            this.addRow(new DrawerRow("General", false, null, () -> {
            }));
            this.addRow(new DrawerRow("Modules", true, null, () -> this.openNestedDrawer(Drawer.MODULES)));
            this.addRow(new DrawerRow("GUI", true, null, () -> this.openNestedDrawer(Drawer.GUI)));
            this.addRow(new DrawerRow("Sound", false, null, () -> {
            }));
            this.addRow(new DrawerRow("Notifications", false, null, () -> {
            }));
            this.addRow(new DrawerRow("GUI Theme", false, () -> true, () -> {
            }));
            this.addBody(new ColorStrip());
        }

        private void buildModules() {
            this.addRow(new DrawerRow("Show disabled modules", false,
                    () -> ModulePanel.this.showDisabledModules,
                    () -> {
                        ModulePanel.this.showDisabledModules = !ModulePanel.this.showDisabledModules;
                        ModulePanel.this.refreshModuleList();
                    }));
            this.addRow(new DrawerRow("Enabled modules first", false,
                    () -> ModulePanel.this.enabledFirst,
                    () -> {
                        ModulePanel.this.enabledFirst = !ModulePanel.this.enabledFirst;
                        ModulePanel.this.refreshModuleList();
                    }));
            this.addRow(new DrawerRow("Show module summaries", false,
                    () -> ModulePanel.this.showSummaries,
                    () -> {
                        ModulePanel.this.showSummaries = !ModulePanel.this.showSummaries;
                        ModulePanel.this.refreshModuleList();
                    }));
        }

        private void buildGui() {
            this.addRow(new DrawerRow("Compact rows", false,
                    () -> ModulePanel.this.compactRows,
                    () -> {
                        ModulePanel.this.compactRows = !ModulePanel.this.compactRows;
                        ModulePanel.this.refreshModuleList();
                    }));
            this.addRow(new DrawerRow("Wide inspector", false,
                    () -> ModulePanel.this.wideInspector,
                    () -> ModulePanel.this.wideInspector = !ModulePanel.this.wideInspector));
            this.addRow(new DrawerRow("GUI style - Central", false, null, () -> {
            }));
        }

        private void addRow(final DrawerRow row) {
            this.addBody(row);
        }

        private void addBody(final Component component) {
            this.body.addChild(component);
        }

        private void openNestedDrawer(final Drawer drawer) {
            ModulePanel.this.drawer = drawer;
            this.refresh();
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
            super(new GridLayout(0, 0));
            this.text = text;
            this.checked = checked;
            this.action = action;
            this.label = textNode(text, () -> arrow || this.checked() ? TEXT : MUTED);
            this.arrow = arrow ? iconNode("chevron-right", FAINT) : null;
            this.toggle = checked == null ? null : new ToggleSwitch(this::checked, this::runAction);
            this.fixedSize(new Size(-1, SettingsDrawer.ROW_HEIGHT));
            this.background.layoutOptions(fillCell(0, 0, 3, 2));
            this.divider.layoutOptions(cell(0, 1, 3, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                    Padding.EMPTY, null, 1F));
            this.label.layoutOptions(cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(12F, 0F, 8F, 0F), null, null));
            this.addChild(this.background);
            this.addChild(this.divider);
            this.addChild(this.label);
            if (this.arrow != null) {
                this.arrow.layoutOptions(cell(2, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                        padding(0F, 0F, 8F, 0F), ICON_BOX, ICON_BOX));
                this.addChild(this.arrow);
            }
            if (this.toggle != null) {
                this.toggle.layoutOptions(cell(2, 0, 1, 1, 0F, 1F, GridAnchor.CENTER, GridFill.NONE,
                        padding(0F, 0F, 14F, 0F), SWITCH_WIDTH, SWITCH_HEIGHT));
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
            super(new GridLayout(0, 0));
            this.message = message;
            this.fixedSize(new Size(-1, 72));
            this.label = textNode(message, FAINT);
            this.background.layoutOptions(fillCell(0, 0));
            this.label.layoutOptions(cell(0, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                    padding(12F, 0F, 12F, 0F), null, null));
            this.addChild(this.background);
            this.addChild(this.label);
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
