package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.lenni0451.commons.color.Color;
import net.lenni0451.commons.math.MathUtils;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.container.Button;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.component.impl.Checkbox;
import net.lenni0451.rivet.component.impl.FormattedLabel;
import net.lenni0451.rivet.component.impl.TextField;
import net.lenni0451.rivet.component.impl.slider.Slider;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayout;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayoutOptions;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.layout.grid.GridLayout;
import net.lenni0451.rivet.layout.grid.GridLayoutOptions;
import net.lenni0451.rivet.layout.list.VerticalListLayout;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextFormat;
import net.lenni0451.rivet.text.model.TextLine;
import net.lenni0451.rivet.text.model.TextOrigin;
import net.lenni0451.rivet.text.model.TextSection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModulePanel extends Container {

    private static final float MIN_WINDOW_WIDTH = 170;
    private static final float MAX_WINDOW_WIDTH = 224;
    private static final float HEADER_HEIGHT = 20;
    private static final float MODULE_HEADER_HEIGHT = 24;
    private static final float BOOLEAN_ROW_HEIGHT = 24;
    private static final float NUMBER_ROW_HEIGHT = 38;
    private static final float SELECT_ROW_HEIGHT = 26;
    private static final float STRING_ROW_HEIGHT = 31;
    private static final float TOGGLE_CELL_WIDTH = 24;
    private static final float PADDING = 6;
    private static final float GAP = 5;
    private static final float INTRO_HEIGHT = HEADER_HEIGHT + PADDING;
    private static final float CONTROL_RADIUS = 2F;
    private static final long WINDOW_HEIGHT_ANIMATION_NANOS = 180_000_000L;
    private static final long FOCUS_PULSE_NANOS = 260_000_000L;

    private static final Color BACKDROP_TOP = Color.fromRGBA(5, 5, 6, 70);
    private static final Color BACKDROP_BOTTOM = Color.fromRGBA(5, 5, 6, 118);
    private static final Color SHADOW = Color.fromRGBA(0, 0, 0, 96);
    private static final Color WINDOW = Color.fromRGBA(17, 17, 19, 186);
    private static final Color WINDOW_ACTIVE = Color.fromRGBA(22, 22, 25, 206);
    private static final Color HEADER = Color.fromRGBA(34, 34, 38, 232);
    private static final Color SURFACE = Color.fromRGBA(25, 25, 28, 164);
    private static final Color SURFACE_HOVER = Color.fromRGBA(34, 34, 38, 186);
    private static final Color SURFACE_DARK = Color.fromRGBA(12, 12, 14, 98);
    private static final Color BORDER = Color.fromRGBA(82, 82, 92, 196);
    private static final Color BORDER_SOFT = Color.fromRGBA(54, 54, 62, 120);
    private static final Color TEXT = Color.fromRGB(236, 232, 224);
    private static final Color MUTED = Color.fromRGB(154, 150, 142);
    private static final Color FAINT = Color.fromRGB(96, 94, 90);
    private static final Color ACTIVE = Color.fromRGB(0, 236, 92);

    private final ModuleManager modules;
    private final ClientConfig config;
    private final Map<ModuleCategory, WindowState> windows = new EnumMap<>(ModuleCategory.class);
    private final Map<ModuleCategory, HeightAnimation> heightAnimations = new EnumMap<>(ModuleCategory.class);
    private final Map<ModuleCategory, CategoryWindow> categoryWindows = new EnumMap<>(ModuleCategory.class);
    private final List<ModuleCategory> zOrder = new ArrayList<>();
    private final Set<String> expandedModules = new HashSet<>();
    private Size lastSize = Size.EMPTY;
    private ModuleCategory draggingCategory;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean animationsActive;

    public ModulePanel(final ModuleManager modules, final ClientConfig config) {
        super(AbsoluteLayout.INSTANCE);
        this.modules = modules;
        this.config = config;
        this.restoreExpandedModules();
        this.restoreZOrder();
        for (ModuleCategory category : this.zOrder) {
            CategoryWindow window = new CategoryWindow(this, category);
            this.categoryWindows.put(category, window);
            this.addChild(window);
        }
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        renderer.fillGradientRect(0, 0, bounds.width(), bounds.height(), BACKDROP_TOP, BACKDROP_TOP, BACKDROP_BOTTOM, BACKDROP_BOTTOM);
        renderer.text(this.rivet().backend().shapeText("AnarchyClient", TEXT), 10, 17, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.BASELINE);
        super.render(renderer, bounds);
        if (this.animationsActive) {
            this.rivet().recalculateNextFrame();
        }
    }

    @Override
    public void computeLayout(final Size size) {
        long now = System.nanoTime();
        this.animationsActive = false;
        this.ensureWindows(size);
        this.applyWindowLayouts(size, now);
        super.computeLayout(size);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return constraints;
    }

    private void startDrag(final ModuleCategory category, final float mouseX, final float mouseY) {
        WindowState window = this.windows.get(category);
        this.bringToFront(category);
        this.draggingCategory = category;
        this.dragOffsetX = mouseX - window.x();
        this.dragOffsetY = mouseY - window.y();
        this.rivet().recalculateNextFrame();
    }

    private boolean drag(final ModuleCategory category, final float mouseX, final float mouseY) {
        if (this.draggingCategory != category) {
            return false;
        }
        WindowState window = this.windows.get(category);
        float width = windowWidth(this.lastSize);
        window.x(clamp(mouseX - this.dragOffsetX, 6, Math.max(6, this.lastSize.width() - width - 6)));
        window.y(clamp(mouseY - this.dragOffsetY, 8, Math.max(8, this.lastSize.height() - HEADER_HEIGHT - 8)));
        this.applyWindowLayouts(this.lastSize, System.nanoTime());
        this.saveWindowState(category);
        this.rivet().recalculateNextFrame();
        return true;
    }

    private boolean stopDrag(final ModuleCategory category) {
        boolean handled = this.draggingCategory == category;
        if (handled) {
            this.draggingCategory = null;
            this.saveWindowState(category);
            this.save();
        }
        return handled;
    }

    private void toggleExpanded(final Module module, final ModuleGroup group) {
        if (!this.expandedModules.remove(module.id())) {
            this.expandedModules.add(module.id());
        }
        this.config.expandedModules(this.expandedModules);
        group.rebuild();
        this.save();
        this.rivet().recalculateNextFrame();
    }

    private void save() {
        this.config.save();
    }

    private boolean isExpanded(final Module module) {
        return this.expandedModules.contains(module.id());
    }

    private void restoreExpandedModules() {
        this.config.expandedModules().ifPresent(this.expandedModules::addAll);
    }

    private void restoreZOrder() {
        this.config.categoryOrder().ifPresentOrElse(
                this.zOrder::addAll,
                () -> this.zOrder.addAll(List.of(ModuleCategory.values()))
        );
    }

    private void saveWindowState(final ModuleCategory category) {
        WindowState window = this.windows.get(category);
        if (window != null) {
            this.config.categoryWindow(category, window.x(), window.y());
        }
    }

    private void ensureWindows(final Size size) {
        if (!this.windows.isEmpty() && this.lastSize.equals(size)) {
            return;
        }
        this.lastSize = size;

        float width = windowWidth(size);
        float margin = 10;
        float stepX = width + GAP + 3;
        int columns = Math.max(1, (int) Math.floor((size.width() - margin * 2 + GAP + 3) / stepX));
        float rowHeight = HEADER_HEIGHT + PADDING * 2 + MODULE_HEADER_HEIGHT + GAP + 18;
        int index = 0;
        for (ModuleCategory category : ModuleCategory.values()) {
            WindowState window = this.windows.computeIfAbsent(category, this::initialWindowState);
            if (window.x() == 0 && window.y() == 0) {
                window.x(margin + (index % columns) * stepX);
                window.y(28 + (index / columns) * rowHeight);
            }
            window.x(clamp(window.x(), margin, Math.max(margin, size.width() - width - margin)));
            window.y(clamp(window.y(), margin, Math.max(margin, size.height() - HEADER_HEIGHT - margin)));
            index++;
        }
    }

    private WindowState initialWindowState(final ModuleCategory category) {
        return this.config.categoryWindow(category)
                .map(state -> new WindowState(state.x(), state.y()))
                .orElseGet(() -> new WindowState(0, 0));
    }

    private void applyWindowLayouts(final Size size, final long now) {
        float width = windowWidth(size);
        for (ModuleCategory category : ModuleCategory.values()) {
            WindowState window = this.windows.get(category);
            float height = this.animatedWindowHeight(category, windowHeight(category, size), now);
            this.categoryWindows.get(category).layoutOptions(new AbsoluteLayoutOptions(window.x(), window.y(), width, height));
        }
    }

    private float animatedWindowHeight(final ModuleCategory category, final float targetHeight, final long now) {
        HeightAnimation animation = this.heightAnimations.computeIfAbsent(category, ignored -> HeightAnimation.intro(targetHeight, now));
        float height = animation.height(targetHeight, now);
        this.animationsActive |= animation.active(now);
        return height;
    }

    private void bringToFront(final ModuleCategory category) {
        this.zOrder.remove(category);
        this.zOrder.add(category);
        this.config.categoryOrder(this.zOrder);
        CategoryWindow window = this.categoryWindows.get(category);
        if (window != null) {
            window.pulseFocus();
        }
        this.sortChildren((left, right) -> {
            ModuleCategory leftCategory = ((CategoryWindow) left).category();
            ModuleCategory rightCategory = ((CategoryWindow) right).category();
            return Integer.compare(this.zOrder.indexOf(leftCategory), this.zOrder.indexOf(rightCategory));
        });
    }

    private boolean isActive(final ModuleCategory category) {
        return this.zOrder.getLast() == category;
    }

    private float categoryContentHeight(final ModuleCategory category) {
        float height = 0;
        for (Module module : this.modules.byCategory(category)) {
            height += MODULE_HEADER_HEIGHT + GAP;
            if (this.isExpanded(module)) {
                for (Setting<?> setting : module.settings()) {
                    height += settingRowHeight(setting);
                }
            }
        }
        return Math.max(height, MODULE_HEADER_HEIGHT);
    }

    private float windowHeight(final ModuleCategory category, final Size size) {
        float maxHeight = Math.max(HEADER_HEIGHT + PADDING * 2, size.height() - 42);
        return Math.min(HEADER_HEIGHT + PADDING * 2 + this.categoryContentHeight(category), maxHeight);
    }

    private static float settingRowHeight(final Setting<?> setting) {
        if (setting instanceof NumberSetting) {
            return NUMBER_ROW_HEIGHT;
        }
        if (setting instanceof StringSetting) {
            return STRING_ROW_HEIGHT;
        }
        if (setting instanceof SelectSetting) {
            return SELECT_ROW_HEIGHT;
        }
        return BOOLEAN_ROW_HEIGHT;
    }

    private static float windowWidth(final Size size) {
        int categoryCount = Math.max(1, ModuleCategory.values().length);
        float available = size.width() - 20 - (categoryCount - 1) * (GAP + 3);
        return clamp(available / categoryCount, MIN_WINDOW_WIDTH, MAX_WINDOW_WIDTH);
    }

    private static float clamp(final double value, final double min, final double max) {
        return (float) MathUtils.clamp(value, min, max);
    }

    private static FormattedLabel label(final String text, final Color color) {
        FormattedLabel label = new FormattedLabel(new TextLine(new TextSection(text, TextFormat.DEFAULT.withColor(color))));
        label.horizontalOrigin(TextOrigin.Horizontal.LOGICAL_LEFT);
        label.verticalOrigin(TextOrigin.Vertical.LOGICAL_CENTER);
        label.interactive(false);
        return label;
    }

    private static Button button(final Component child, final Button.ClickListener listener) {
        Button button = new Button(child, listener);
        button.cornerRadius().set(CONTROL_RADIUS);
        button.outlineWidth().set(1F);
        button.inactiveColor().set(SURFACE);
        button.inactiveOutlineColor().set(BORDER_SOFT);
        button.activeColor().set(SURFACE_HOVER);
        button.activeOutlineColor().set(BORDER);
        button.clickColor().set(SURFACE_DARK);
        button.clickOutlineColor().set(BORDER);
        button.innerPadding().set(new Padding(6, 3, 6, 3));
        button.clickOn().set(Button.ClickOn.UP);
        return button;
    }

    private static Checkbox checkbox(final boolean checked) {
        Checkbox checkbox = new Checkbox(checked);
        checkbox.backgroundColor().set(SURFACE_DARK);
        checkbox.outlineColor().set(BORDER);
        checkbox.checkColor().set(ACTIVE);
        checkbox.cornerRadius().set(CONTROL_RADIUS);
        checkbox.fixedSize(new Size(18, 18));
        return checkbox;
    }

    private static GridLayoutOptions toggleCell(final int column, final float rowHeight) {
        return new GridLayoutOptions(column, 0)
                .withAnchor(GridAnchor.CENTER)
                .withWidth(TOGGLE_CELL_WIDTH)
                .withHeight(rowHeight);
    }

    private static Slider slider(final NumberSetting setting) {
        Slider slider = new Slider(setting.min(), setting.max(), setting.step(), setting.value());
        slider.barColor().set(Color.fromRGBA(50, 50, 56, 210));
        slider.activeBarColor().set(ACTIVE);
        slider.thumbColor().set(TEXT);
        slider.thumbClickColor().set(ACTIVE);
        slider.barHeight().set(3F);
        slider.barCornerRadius().set(CONTROL_RADIUS);
        slider.thumbWidth().set(8F);
        slider.thumbHeight().set(8F);
        slider.thumbCornerRadius().set(CONTROL_RADIUS);
        slider.fixedSize(new Size(-1, 14));
        return slider;
    }

    private final class CategoryWindow extends Container {

        private final ModuleCategory category;
        private final WindowHeader header;
        private final ScrollContainer scroll;
        private long focusPulseStartNanos;

        private CategoryWindow(final ModulePanel panel, final ModuleCategory category) {
            super(AbsoluteLayout.INSTANCE);
            this.category = category;
            this.header = new WindowHeader(panel, category);
            Container list = new Container(new VerticalListLayout((int) GAP, true));
            for (Module module : ModulePanel.this.modules.byCategory(category)) {
                list.addChild(new ModuleGroup(module));
            }
            this.scroll = new ScrollContainer(list);
            this.scroll.barColor().set(Color.fromRGBA(154, 150, 142, 80));
            this.scroll.barHoverColor().set(Color.fromRGBA(154, 150, 142, 130));
            this.scroll.barClickColor().set(Color.fromRGBA(0, 236, 92, 170));
            this.scroll.scrollSpeed().set(18F);
            this.addChild(this.header);
            this.addChild(this.scroll);
        }

        private ModuleCategory category() {
            return this.category;
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            boolean active = ModulePanel.this.isActive(this.category);
            float pulse = this.focusPulse();
            renderer.fillRect(2, 3, bounds.width(), bounds.height(), SHADOW);
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), active ? WINDOW_ACTIVE : WINDOW);
            super.render(renderer, bounds);
            if (pulse > 0) {
                renderer.fillRect(0, HEADER_HEIGHT, 1 + pulse * 2, Math.max(0, bounds.height() - HEADER_HEIGHT), ACTIVE.multiplyAlpha(0.42F * pulse));
                renderer.fillRect(0, 0, bounds.width(), 1, ACTIVE.multiplyAlpha(0.22F * pulse));
            }
            renderer.outlineRect(0, 0, bounds.width(), bounds.height(), 1, Color.interpolate(pulse, active ? BORDER : BORDER_SOFT, ACTIVE));
            ModulePanel.this.animationsActive |= pulse > 0;
        }

        private void pulseFocus() {
            this.focusPulseStartNanos = System.nanoTime();
        }

        private float focusPulse() {
            if (this.focusPulseStartNanos == 0) {
                return 0;
            }
            float progress = animationProgress(this.focusPulseStartNanos, FOCUS_PULSE_NANOS, System.nanoTime());
            return progress >= 1 ? 0 : 1 - easeOutCubic(progress);
        }

        @Override
        public void computeLayout(final Size size) {
            this.header.layoutOptions(new AbsoluteLayoutOptions(0, 0, size.width(), HEADER_HEIGHT));
            this.scroll.layoutOptions(new AbsoluteLayoutOptions(
                    PADDING,
                    HEADER_HEIGHT + PADDING,
                    Math.max(0, size.width() - PADDING * 2),
                    Math.max(0, size.height() - HEADER_HEIGHT - PADDING * 2)
            ));
            super.computeLayout(size);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return constraints;
        }
    }

    private static final class WindowHeader extends Component {

        private final ModulePanel panel;
        private final ModuleCategory category;

        private WindowHeader(final ModulePanel panel, final ModuleCategory category) {
            this.panel = panel;
            this.category = category;
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), HEADER);
            renderer.fillRect(0, 0, 3, bounds.height(), ACTIVE);
            renderer.fillRect(8, 8, 10, 1, FAINT);
            renderer.fillRect(8, 11, 10, 1, FAINT);
            renderer.text(this.rivet().backend().shapeText(this.category.displayName(), TEXT), 22, 14, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.BASELINE);
        }

        @Override
        protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
            if (event.button() != MouseButton.LEFT) {
                return false;
            }
            this.panel.startDrag(this.category, bounds.x() + event.x(), bounds.y() + event.y());
            return true;
        }

        @Override
        protected boolean onComponentMouseMove(final MouseMoveEvent event, final Rectangle bounds) {
            return this.panel.drag(this.category, bounds.x() + event.x(), bounds.y() + event.y());
        }

        @Override
        protected boolean onComponentMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
            return event.button() == MouseButton.LEFT && this.panel.stopDrag(this.category);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(MIN_WINDOW_WIDTH, HEADER_HEIGHT);
        }
    }

    private final class ModuleGroup extends Container {

        private final Module module;

        private ModuleGroup(final Module module) {
            super(new VerticalListLayout(0, true));
            this.module = module;
            this.rebuild();
        }

        private void rebuild() {
            this.clearChildren();
            this.addChild(new ModuleHeader(this.module, this));
            if (ModulePanel.this.isExpanded(this.module)) {
                int index = 0;
                for (Setting<?> setting : this.module.settings()) {
                    this.addChild(settingRow(setting, index++));
                }
            }
            if (this.parent() != null) {
                this.parent().requestLayoutRecalculation();
            }
        }

        private Component settingRow(final Setting<?> setting, final int index) {
            if (setting instanceof BooleanSetting bool) {
                return new BooleanSettingRow(bool, index);
            }
            if (setting instanceof NumberSetting number) {
                return new NumberSettingRow(number, index);
            }
            if (setting instanceof SelectSetting select) {
                return new SelectSettingRow(select, index);
            }
            if (setting instanceof StringSetting string) {
                return new StringSettingRow(string, index);
            }
            return new ValueSettingRow(setting, index);
        }
    }

    private final class ModuleHeader extends Container {

        private ModuleHeader(final Module module, final ModuleGroup group) {
            super(new GridLayout(4, 0));
            this.fixedSize(new Size(-1, MODULE_HEADER_HEIGHT));
            FormattedLabel name = label((ModulePanel.this.isExpanded(module) ? "- " : "+ ") + module.name(), TEXT);
            Button expand = button(name, ignored -> ModulePanel.this.toggleExpanded(module, group));
            expand.layoutOptions(new GridLayoutOptions(0, 0).withFill(GridFill.BOTH).withWeightX(1).withHeight(MODULE_HEADER_HEIGHT));
            Checkbox enabled = checkbox(module.enabled());
            enabled.toggleListener().add(value -> {
                module.enabled(value);
                ModulePanel.this.save();
            });
            enabled.layoutOptions(toggleCell(1, MODULE_HEADER_HEIGHT));
            this.addChild(expand);
            this.addChild(enabled);
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), SURFACE);
            super.render(renderer, bounds);
            renderer.line(0, bounds.height(), bounds.width(), bounds.height(), 1, BORDER_SOFT);
        }
    }

    private abstract static class SettingRow extends Container {

        private final int index;
        private final float height;

        private SettingRow(final int index, final float height) {
            super(new GridLayout(6, 0));
            this.index = index;
            this.height = height;
            this.fixedSize(new Size(-1, height));
        }

        @Override
        public void render(final Renderer renderer, final Rectangle bounds) {
            renderer.fillRect(0, 0, bounds.width(), bounds.height(), this.index % 2 == 0 ? SURFACE_DARK : WINDOW);
            super.render(renderer, bounds);
        }

        @Override
        public Size computeIdealSize(final Size constraints) {
            return new Size(constraints.width(), this.height);
        }
    }

    private final class BooleanSettingRow extends SettingRow {

        private BooleanSettingRow(final BooleanSetting setting, final int index) {
            super(index, BOOLEAN_ROW_HEIGHT);
            FormattedLabel name = label(setting.name(), MUTED);
            name.layoutOptions(new GridLayoutOptions(0, 0).withFill(GridFill.HORIZONTAL).withWeightX(1).withAnchor(GridAnchor.LEFT).withHeight(BOOLEAN_ROW_HEIGHT));
            Checkbox value = checkbox(setting.value());
            value.toggleListener().add(checked -> {
                setting.value(checked);
                ModulePanel.this.save();
            });
            value.layoutOptions(toggleCell(1, BOOLEAN_ROW_HEIGHT));
            this.addChild(name);
            this.addChild(value);
        }
    }

    private final class NumberSettingRow extends SettingRow {

        private NumberSettingRow(final NumberSetting setting, final int index) {
            super(index, NUMBER_ROW_HEIGHT);
            FormattedLabel name = label(setting.name(), MUTED);
            name.layoutOptions(new GridLayoutOptions(0, 0).withFill(GridFill.HORIZONTAL).withWeightX(1).withHeight(16F));
            FormattedLabel value = label(SettingControls.displayValue(setting), TEXT);
            value.horizontalOrigin(TextOrigin.Horizontal.VISUAL_RIGHT);
            value.layoutOptions(new GridLayoutOptions(1, 0).withAnchor(GridAnchor.RIGHT).withHeight(16F));
            Slider slider = slider(setting);
            slider.valueChangeListener().add(number -> {
                setting.value(number);
                value.text(new TextLine(new TextSection(SettingControls.displayValue(setting), TextFormat.DEFAULT.withColor(TEXT))));
                ModulePanel.this.save();
            });
            slider.layoutOptions(new GridLayoutOptions(0, 1).withColumnSpan(2).withFill(GridFill.HORIZONTAL).withWeightX(1).withHeight(18F));
            this.addChild(name);
            this.addChild(value);
            this.addChild(slider);
        }
    }

    private final class SelectSettingRow extends SettingRow {

        private SelectSettingRow(final SelectSetting setting, final int index) {
            super(index, SELECT_ROW_HEIGHT);
            FormattedLabel name = label(setting.name(), MUTED);
            name.layoutOptions(new GridLayoutOptions(0, 0).withFill(GridFill.HORIZONTAL).withWeightX(1).withHeight(SELECT_ROW_HEIGHT));
            FormattedLabel current = label(setting.value(), TEXT);
            current.horizontalOrigin(TextOrigin.Horizontal.VISUAL_RIGHT);
            Button value = button(current, ignored -> {
                setting.next();
                current.text(new TextLine(new TextSection(setting.value(), TextFormat.DEFAULT.withColor(TEXT))));
                ModulePanel.this.save();
            });
            value.layoutOptions(new GridLayoutOptions(1, 0).withFill(GridFill.HORIZONTAL).withWeightX(1).withWidth(72F).withHeight(SELECT_ROW_HEIGHT - 4));
            this.addChild(name);
            this.addChild(value);
        }
    }

    private final class StringSettingRow extends SettingRow {

        private StringSettingRow(final StringSetting setting, final int index) {
            super(index, STRING_ROW_HEIGHT);
            FormattedLabel name = label(setting.name(), MUTED);
            name.layoutOptions(new GridLayoutOptions(0, 0).withFill(GridFill.HORIZONTAL).withWeightX(1).withHeight(STRING_ROW_HEIGHT));
            TextField field = new TextField(setting.value());
            field.backgroundColor().set(SURFACE_DARK);
            field.outlineColor().set(BORDER_SOFT);
            field.focusedOutlineColor().set(ACTIVE);
            field.cursorColor().set(TEXT);
            field.selectionColor().set(Color.fromRGBA(0, 236, 92, 90));
            field.cornerRadius().set(CONTROL_RADIUS);
            field.valueChangeListener().add(value -> {
                setting.value(value);
                ModulePanel.this.save();
            });
            field.layoutOptions(new GridLayoutOptions(1, 0).withFill(GridFill.HORIZONTAL).withWeightX(1).withWidth(96F).withHeight(STRING_ROW_HEIGHT - 6));
            this.addChild(name);
            this.addChild(field);
        }
    }

    private static final class ValueSettingRow extends SettingRow {

        private ValueSettingRow(final Setting<?> setting, final int index) {
            super(index, BOOLEAN_ROW_HEIGHT);
            FormattedLabel name = label(setting.name(), MUTED);
            name.layoutOptions(new GridLayoutOptions(0, 0).withFill(GridFill.HORIZONTAL).withWeightX(1).withHeight(BOOLEAN_ROW_HEIGHT));
            FormattedLabel value = label(SettingControls.displayValue(setting), TEXT);
            value.horizontalOrigin(TextOrigin.Horizontal.VISUAL_RIGHT);
            value.layoutOptions(new GridLayoutOptions(1, 0).withAnchor(GridAnchor.RIGHT).withHeight(BOOLEAN_ROW_HEIGHT));
            this.addChild(name);
            this.addChild(value);
        }
    }

    private static final class WindowState {

        private float x;
        private float y;

        private WindowState(final float x, final float y) {
            this.x = x;
            this.y = y;
        }

        private float x() {
            return this.x;
        }

        private void x(final float x) {
            this.x = x;
        }

        private float y() {
            return this.y;
        }

        private void y(final float y) {
            this.y = y;
        }
    }

    private static final class HeightAnimation {

        private float start;
        private float current;
        private float target;
        private long startedAt;

        private HeightAnimation(final float start, final float target, final long now) {
            this.start = start;
            this.current = start;
            this.target = target;
            this.startedAt = now;
        }

        private static HeightAnimation intro(final float target, final long now) {
            return new HeightAnimation(Math.min(target, INTRO_HEIGHT), target, now);
        }

        private float height(final float targetHeight, final long now) {
            if (Math.abs(this.target - targetHeight) > 0.5F) {
                this.start = this.current;
                this.target = targetHeight;
                this.startedAt = now;
            }
            float progress = animationProgress(this.startedAt, WINDOW_HEIGHT_ANIMATION_NANOS, now);
            this.current = lerp(this.start, this.target, easeOutCubic(progress));
            if (progress >= 1 || Math.abs(this.current - this.target) <= 0.5F) {
                this.current = this.target;
            }
            return this.current;
        }

        private boolean active(final long now) {
            return Math.abs(this.current - this.target) > 0.5F
                    && animationProgress(this.startedAt, WINDOW_HEIGHT_ANIMATION_NANOS, now) < 1;
        }
    }

    private static float animationProgress(final long startedAt, final long durationNanos, final long now) {
        if (startedAt == 0 || durationNanos <= 0) {
            return 1;
        }
        return clamp((double) (now - startedAt) / durationNanos, 0, 1);
    }

    private static float easeOutCubic(final float progress) {
        float inverse = 1 - progress;
        return 1 - inverse * inverse * inverse;
    }

    private static float lerp(final float start, final float end, final float progress) {
        return start + (end - start) * progress;
    }
}
