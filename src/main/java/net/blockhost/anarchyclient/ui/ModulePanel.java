package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.Setting;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.input.mouse.MouseScrollEvent;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModulePanel extends Component {

    private static final float WINDOW_WIDTH = 316;
    private static final float HEADER_HEIGHT = 28;
    private static final float MODULE_HEADER_HEIGHT = 34;
    private static final float SETTING_ROW_HEIGHT = 40;
    private static final float PADDING = 8;
    private static final float GAP = 8;
    private static final float SWITCH_WIDTH = 38;
    private static final float SWITCH_HEIGHT = 16;
    private static final float SLIDER_HEIGHT = 5;

    private static final Color BACKDROP_TOP = Color.fromRGBA(5, 5, 6, 70);
    private static final Color BACKDROP_BOTTOM = Color.fromRGBA(5, 5, 6, 118);
    private static final Color SHADOW = Color.fromRGBA(0, 0, 0, 96);
    private static final Color WINDOW = Color.fromRGBA(17, 17, 19, 196);
    private static final Color WINDOW_ACTIVE = Color.fromRGBA(22, 22, 25, 218);
    private static final Color HEADER = Color.fromRGBA(34, 34, 38, 232);
    private static final Color SURFACE = Color.fromRGBA(25, 25, 28, 184);
    private static final Color SURFACE_DARK = Color.fromRGBA(12, 12, 14, 126);
    private static final Color BORDER = Color.fromRGBA(82, 82, 92, 222);
    private static final Color BORDER_SOFT = Color.fromRGBA(54, 54, 62, 180);
    private static final Color TEXT = Color.fromRGB(236, 232, 224);
    private static final Color MUTED = Color.fromRGB(154, 150, 142);
    private static final Color FAINT = Color.fromRGB(96, 94, 90);
    private static final Color ACTIVE = Color.fromRGB(0, 212, 170);
    private static final Color OFF = Color.fromRGB(95, 98, 106);
    private static final Color TRACK = Color.fromRGBA(50, 50, 56, 210);

    private final ModuleManager modules;
    private final ClientConfig config;
    private final Map<ModuleCategory, WindowState> windows = new EnumMap<>(ModuleCategory.class);
    private final List<ModuleCategory> zOrder = new ArrayList<>();
    private final Set<String> expandedModules = new HashSet<>();
    private Size lastSize = Size.EMPTY;
    private ModuleCategory draggingCategory;
    private float dragOffsetX;
    private float dragOffsetY;
    private NumberDrag activeNumberDrag;

    public ModulePanel(final ModuleManager modules, final ClientConfig config) {
        this.modules = modules;
        this.config = config;
        for (ModuleCategory category : ModuleCategory.values()) {
            this.zOrder.add(category);
        }
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        this.ensureWindows(bounds.size());

        renderer.fillGradientRect(0, 0, bounds.width(), bounds.height(), BACKDROP_TOP, BACKDROP_TOP, BACKDROP_BOTTOM, BACKDROP_BOTTOM);
        text(renderer, "AnarchyClient", 18, 28, TEXT);

        for (ModuleCategory category : this.zOrder) {
            this.renderWindow(renderer, bounds, category);
        }
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        if (event.button() != MouseButton.LEFT) {
            return false;
        }
        this.ensureWindows(bounds.size());

        for (int index = this.zOrder.size() - 1; index >= 0; index--) {
            ModuleCategory category = this.zOrder.get(index);
            WindowState window = this.windows.get(category);
            WindowLayout layout = this.windowLayout(category, bounds);
            if (!layout.bounds().contains(event.x(), event.y())) {
                continue;
            }

            this.bringToFront(category);
            if (layout.header().contains(event.x(), event.y())) {
                this.draggingCategory = category;
                this.dragOffsetX = event.x() - window.x();
                this.dragOffsetY = event.y() - window.y();
                this.rivet().recalculateNextFrame();
                return true;
            }

            ClickTarget target = this.findTarget(layout, event.x(), event.y());
            if (target == null) {
                return true;
            }
            if (target.kind() == TargetKind.MODULE_TOGGLE) {
                target.module().toggle();
                this.config.save();
                this.rivet().recalculateNextFrame();
                return true;
            }
            if (target.kind() == TargetKind.MODULE_EXPAND) {
                this.toggleExpanded(target.module());
                this.rivet().recalculateNextFrame();
                return true;
            }
            if (target.setting() instanceof BooleanSetting bool) {
                bool.value(!bool.value());
                this.config.save();
                this.rivet().recalculateNextFrame();
                return true;
            }
            if (target.setting() instanceof NumberSetting number) {
                this.activeNumberDrag = new NumberDrag(number, target.controlBounds());
                this.setNumberFromMouse(number, target.controlBounds(), event.x());
                this.config.save();
                this.rivet().recalculateNextFrame();
                return true;
            }
            return true;
        }
        return true;
    }

    @Override
    protected boolean onComponentMouseMove(final MouseMoveEvent event, final Rectangle bounds) {
        if (this.draggingCategory != null) {
            WindowState window = this.windows.get(this.draggingCategory);
            window.x(clamp(event.x() - this.dragOffsetX, 8, Math.max(8, bounds.width() - WINDOW_WIDTH - 8)));
            window.y(clamp(event.y() - this.dragOffsetY, 8, Math.max(8, bounds.height() - HEADER_HEIGHT - 8)));
            this.rivet().recalculateNextFrame();
            return true;
        }
        if (this.activeNumberDrag != null) {
            this.setNumberFromMouse(this.activeNumberDrag.setting(), this.activeNumberDrag.bounds(), event.x());
            this.config.save();
            this.rivet().recalculateNextFrame();
            return true;
        }
        return false;
    }

    @Override
    protected boolean onComponentMouseUp(final MouseButtonEvent event, final Rectangle bounds) {
        if (event.button() != MouseButton.LEFT) {
            return false;
        }
        boolean handled = this.draggingCategory != null || this.activeNumberDrag != null;
        this.draggingCategory = null;
        this.activeNumberDrag = null;
        return handled;
    }

    @Override
    protected boolean onComponentMouseScroll(final MouseScrollEvent event, final Rectangle bounds) {
        this.ensureWindows(bounds.size());
        for (int index = this.zOrder.size() - 1; index >= 0; index--) {
            ModuleCategory category = this.zOrder.get(index);
            WindowLayout layout = this.windowLayout(category, bounds);
            if (!layout.bounds().contains(event.x(), event.y())) {
                continue;
            }
            WindowState window = this.windows.get(category);
            float maxScroll = Math.max(0, layout.contentHeight() - layout.viewport().height());
            window.scroll(clamp(window.scroll() - event.scrollY() * 18, 0, maxScroll));
            this.rivet().recalculateNextFrame();
            return true;
        }
        return false;
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return constraints;
    }

    private void renderWindow(final Renderer renderer, final Rectangle screen, final ModuleCategory category) {
        WindowLayout layout = this.windowLayout(category, screen);
        WindowState window = this.windows.get(category);
        boolean active = this.zOrder.getLast() == category;

        renderer.fillRect(layout.bounds().x() + 4, layout.bounds().y() + 5, layout.bounds().width(), layout.bounds().height(), SHADOW);
        renderer.fillRect(layout.bounds().x(), layout.bounds().y(), layout.bounds().width(), layout.bounds().height(), active ? WINDOW_ACTIVE : WINDOW);
        renderer.outlineRect(layout.bounds().x(), layout.bounds().y(), layout.bounds().width(), layout.bounds().height(), 1, active ? BORDER : BORDER_SOFT);
        renderer.fillRect(layout.header().x(), layout.header().y(), layout.header().width(), layout.header().height(), HEADER);
        renderer.fillRect(layout.header().x(), layout.header().y(), 3, layout.header().height(), ACTIVE);

        text(renderer, category.displayName(), layout.header().x() + 12, layout.header().y() + 18, TEXT);
        text(renderer, Integer.toString(this.modules.byCategory(category).size()), layout.header().maxX() - 20, layout.header().y() + 18, MUTED);

        renderer.scissor(layout.viewport().x(), layout.viewport().y(), layout.viewport().width(), layout.viewport().height(), () -> {
            float y = layout.contentY() - window.scroll();
            for (Module module : this.modules.byCategory(category)) {
                y = this.renderModule(renderer, layout, module, y);
            }
        });
    }

    private float renderModule(final Renderer renderer, final WindowLayout layout, final Module module, final float y) {
        Rectangle moduleHeader = new Rectangle(layout.contentX(), y, layout.contentWidth(), MODULE_HEADER_HEIGHT);
        boolean expanded = this.isExpanded(module);

        renderer.fillRect(moduleHeader.x(), moduleHeader.y(), moduleHeader.width(), moduleHeader.height(), SURFACE);
        renderer.outlineRect(moduleHeader.x(), moduleHeader.y(), moduleHeader.width(), moduleHeader.height(), 1, BORDER_SOFT);
        text(renderer, expanded ? "-" : "+", moduleHeader.x() + 10, moduleHeader.y() + 21, MUTED);
        text(renderer, module.name(), moduleHeader.x() + 28, moduleHeader.y() + 21, TEXT);
        this.renderSwitch(renderer, moduleHeader.maxX() - SWITCH_WIDTH - 10, moduleHeader.y() + 9, module.enabled());

        float nextY = moduleHeader.maxY();
        if (!expanded) {
            return nextY + GAP;
        }

        List<Setting<?>> settings = module.settings();
        for (int index = 0; index < settings.size(); index++) {
            Setting<?> setting = settings.get(index);
            Rectangle row = new Rectangle(layout.contentX(), nextY, layout.contentWidth(), SETTING_ROW_HEIGHT);
            renderer.fillRect(row.x(), row.y(), row.width(), row.height(), index % 2 == 0 ? SURFACE_DARK : WINDOW);
            renderer.line(row.x() + 12, row.y(), row.maxX() - 12, row.y(), 1, BORDER_SOFT);
            this.renderSetting(renderer, row, setting);
            nextY = row.maxY();
        }
        renderer.line(layout.contentX(), nextY, layout.contentX() + layout.contentWidth(), nextY, 1, BORDER_SOFT);
        return nextY + GAP;
    }

    private void renderSetting(final Renderer renderer, final Rectangle row, final Setting<?> setting) {
        if (setting instanceof BooleanSetting bool) {
            text(renderer, setting.name(), row.x() + 12, row.y() + 24, MUTED);
            this.renderSwitch(renderer, row.maxX() - SWITCH_WIDTH - 12, row.y() + 12, bool.value());
            return;
        }
        if (setting instanceof NumberSetting number) {
            text(renderer, setting.name(), row.x() + 12, row.y() + 17, MUTED);
            text(renderer, SettingControls.displayValue(setting), row.maxX() - 54, row.y() + 17, TEXT);
            Rectangle slider = sliderBounds(row);
            renderer.fillRect(slider.x(), slider.y(), slider.width(), slider.height(), TRACK);
            float fill = slider.width() * numberProgress(number);
            renderer.fillRect(slider.x(), slider.y(), fill, slider.height(), ACTIVE);
            renderer.fillRect(slider.x() + fill - 2, slider.y() - 3, 4, slider.height() + 6, TEXT);
            return;
        }
        text(renderer, setting.name(), row.x() + 12, row.y() + 24, MUTED);
        text(renderer, SettingControls.displayValue(setting), row.x() + 118, row.y() + 24, TEXT);
    }

    private void renderSwitch(final Renderer renderer, final float x, final float y, final boolean enabled) {
        renderer.fillRect(x, y, SWITCH_WIDTH, SWITCH_HEIGHT, enabled ? ACTIVE : OFF);
        renderer.fillRect(x + (enabled ? SWITCH_WIDTH - 14 : 2), y + 2, 12, 12, TEXT);
    }

    private ClickTarget findTarget(final WindowLayout layout, final float mouseX, final float mouseY) {
        WindowState window = this.windows.get(layout.category());
        float y = layout.contentY() - window.scroll();
        for (Module module : this.modules.byCategory(layout.category())) {
            Rectangle moduleHeader = new Rectangle(layout.contentX(), y, layout.contentWidth(), MODULE_HEADER_HEIGHT);
            Rectangle toggle = new Rectangle(moduleHeader.maxX() - SWITCH_WIDTH - 10, moduleHeader.y() + 7, SWITCH_WIDTH + 10, MODULE_HEADER_HEIGHT - 14);
            if (toggle.contains(mouseX, mouseY)) {
                return new ClickTarget(TargetKind.MODULE_TOGGLE, module, null, toggle);
            }
            if (moduleHeader.contains(mouseX, mouseY)) {
                return new ClickTarget(TargetKind.MODULE_EXPAND, module, null, moduleHeader);
            }
            y = moduleHeader.maxY();
            if (this.isExpanded(module)) {
                for (Setting<?> setting : module.settings()) {
                    Rectangle row = new Rectangle(layout.contentX(), y, layout.contentWidth(), SETTING_ROW_HEIGHT);
                    if (row.contains(mouseX, mouseY)) {
                        Rectangle control = setting instanceof NumberSetting ? sliderBounds(row) : row;
                        return new ClickTarget(TargetKind.SETTING, module, setting, control);
                    }
                    y = row.maxY();
                }
            }
            y += GAP;
        }
        return null;
    }

    private WindowLayout windowLayout(final ModuleCategory category, final Rectangle screen) {
        WindowState window = this.windows.get(category);
        float contentHeight = this.categoryContentHeight(category);
        float maxHeight = Math.max(HEADER_HEIGHT + PADDING * 2, screen.height() - 42);
        float height = Math.min(HEADER_HEIGHT + PADDING * 2 + contentHeight, maxHeight);
        Rectangle bounds = new Rectangle(window.x(), window.y(), WINDOW_WIDTH, height);
        Rectangle header = new Rectangle(bounds.x(), bounds.y(), bounds.width(), HEADER_HEIGHT);
        Rectangle viewport = new Rectangle(bounds.x() + PADDING, bounds.y() + HEADER_HEIGHT + PADDING, bounds.width() - PADDING * 2, height - HEADER_HEIGHT - PADDING * 2);
        return new WindowLayout(category, bounds, header, viewport, contentHeight);
    }

    private float categoryContentHeight(final ModuleCategory category) {
        float height = 0;
        for (Module module : this.modules.byCategory(category)) {
            height += MODULE_HEADER_HEIGHT + GAP;
            if (this.isExpanded(module)) {
                height += module.settings().size() * SETTING_ROW_HEIGHT;
            }
        }
        return Math.max(height, MODULE_HEADER_HEIGHT);
    }

    private boolean isExpanded(final Module module) {
        return this.expandedModules.contains(module.id());
    }

    private void toggleExpanded(final Module module) {
        if (!this.expandedModules.remove(module.id())) {
            this.expandedModules.add(module.id());
        }
    }

    private void ensureWindows(final Size size) {
        if (!this.windows.isEmpty() && this.lastSize.equals(size)) {
            return;
        }
        this.lastSize = size;

        float x = 24;
        float y = 52;
        int index = 0;
        for (ModuleCategory category : ModuleCategory.values()) {
            boolean newWindow = !this.windows.containsKey(category);
            WindowState window = this.windows.computeIfAbsent(category, ignored -> new WindowState(0, 0, 0));
            if (window.x() == 0 && window.y() == 0) {
                window.x(x + index * (WINDOW_WIDTH + 14));
                window.y(y + index * 18);
            }
            window.x(clamp(window.x(), 8, Math.max(8, size.width() - WINDOW_WIDTH - 8)));
            window.y(clamp(window.y(), 8, Math.max(8, size.height() - HEADER_HEIGHT - 8)));
            if (newWindow) {
                this.expandedModules.addAll(this.modules.byCategory(category).stream().map(Module::id).toList());
            }
            index++;
        }
    }

    private void bringToFront(final ModuleCategory category) {
        this.zOrder.remove(category);
        this.zOrder.add(category);
    }

    private void setNumberFromMouse(final NumberSetting setting, final Rectangle slider, final float mouseX) {
        SettingControls.setNumberFromProgress(setting, (mouseX - slider.x()) / slider.width());
    }

    private static Rectangle sliderBounds(final Rectangle row) {
        return new Rectangle(row.x() + 12, row.y() + 28, row.width() - 24, SLIDER_HEIGHT);
    }

    private static float numberProgress(final NumberSetting setting) {
        double range = setting.max() - setting.min();
        if (range <= 0) {
            return 0;
        }
        return (float) clamp((setting.value() - setting.min()) / range, 0, 1);
    }

    private void text(final Renderer renderer, final String text, final float x, final float baselineY, final Color color) {
        renderer.text(this.rivet().backend().shapeText(text, color), x, baselineY, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.BASELINE);
    }

    private static float clamp(final double value, final double min, final double max) {
        return (float) Math.max(min, Math.min(max, value));
    }

    private record WindowLayout(ModuleCategory category, Rectangle bounds, Rectangle header, Rectangle viewport, float contentHeight) {

        float contentX() {
            return this.viewport.x();
        }

        float contentY() {
            return this.viewport.y();
        }

        float contentWidth() {
            return this.viewport.width();
        }
    }

    private record ClickTarget(TargetKind kind, Module module, Setting<?> setting, Rectangle controlBounds) {
    }

    private record NumberDrag(NumberSetting setting, Rectangle bounds) {
    }

    private enum TargetKind {
        MODULE_TOGGLE,
        MODULE_EXPAND,
        SETTING
    }

    private static final class WindowState {

        private float x;
        private float y;
        private float scroll;

        private WindowState(final float x, final float y, final float scroll) {
            this.x = x;
            this.y = y;
            this.scroll = scroll;
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

        private float scroll() {
            return this.scroll;
        }

        private void scroll(final float scroll) {
            this.scroll = scroll;
        }
    }
}
