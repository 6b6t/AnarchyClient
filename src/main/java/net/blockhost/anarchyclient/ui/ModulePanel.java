package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.Setting;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.backend.render.Renderer;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

import java.util.List;

public final class ModulePanel extends Component {

    private static final float PANEL_MAX_WIDTH = 760;
    private static final float PANEL_MAX_HEIGHT = 480;
    private static final float PANEL_MARGIN = 48;
    private static final float SIDEBAR_WIDTH = 156;
    private static final float CATEGORY_TOP = 76;
    private static final float CATEGORY_HEIGHT = 24;
    private static final float CATEGORY_GAP = 30;
    private static final float CONTENT_INSET = 18;
    private static final float CONTENT_TOP = 24;
    private static final float MODULE_BASE_HEIGHT = 48;
    private static final float MODULE_GAP = 10;
    private static final float SETTING_HEIGHT = 22;

    private static final Color BACKDROP = Color.fromRGBA(12, 12, 14, 236);
    private static final Color PANEL = Color.fromRGB(28, 28, 31);
    private static final Color PANEL_DARK = Color.fromRGB(18, 18, 20);
    private static final Color BORDER = Color.fromRGB(58, 58, 64);
    private static final Color TEXT = Color.fromRGB(232, 226, 216);
    private static final Color MUTED = Color.fromRGB(156, 150, 142);
    private static final Color ACTIVE = Color.fromRGB(0, 212, 170);
    private static final Color DANGER = Color.fromRGB(255, 107, 157);

    private final ModuleManager modules;
    private final ClientConfig config;
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;

    public ModulePanel(final ModuleManager modules, final ClientConfig config) {
        this.modules = modules;
        this.config = config;
    }

    @Override
    public void render(final Renderer renderer, final Rectangle bounds) {
        PanelLayout layout = PanelLayout.from(bounds);
        renderer.fillRect(0, 0, bounds.width(), bounds.height(), BACKDROP);

        renderer.fillRect(layout.x(), layout.y(), layout.panelWidth(), layout.panelHeight(), PANEL);
        renderer.outlineRect(layout.x(), layout.y(), layout.panelWidth(), layout.panelHeight(), 1, BORDER);
        renderer.fillRect(layout.x(), layout.y(), SIDEBAR_WIDTH, layout.panelHeight(), PANEL_DARK);
        renderer.line(layout.contentDividerX(), layout.y(), layout.contentDividerX(), layout.y() + layout.panelHeight(), 1, BORDER);

        text(renderer, "AnarchyClient", layout.x() + 16, layout.y() + 24, TEXT);
        text(renderer, "Fabric 26.1.2", layout.x() + 16, layout.y() + 42, MUTED);

        for (ModuleCategory category : ModuleCategory.values()) {
            boolean selected = this.selectedCategory == category;
            Rectangle categoryBounds = layout.categoryBounds(category);
            if (selected) {
                renderer.fillRect(categoryBounds.x(), categoryBounds.y(), categoryBounds.width(), categoryBounds.height(), Color.fromRGB(40, 40, 44));
                renderer.fillRect(categoryBounds.x(), categoryBounds.y(), 3, categoryBounds.height(), ACTIVE);
            }
            text(renderer, category.displayName(), layout.x() + 18, categoryBounds.y() + 14, selected ? TEXT : MUTED);
        }

        text(renderer, this.selectedCategory.displayName(), layout.contentX(), layout.contentY(), TEXT);

        List<Module> visibleModules = this.modules.byCategory(this.selectedCategory);
        float contentY = layout.firstModuleY();
        for (Module module : visibleModules) {
            List<Setting<?>> settings = module.settings();
            ModuleRow row = layout.moduleRow(contentY, settings.size());
            renderer.fillRect(row.bounds().x(), row.bounds().y(), row.bounds().width(), row.bounds().height(), PANEL_DARK);
            renderer.outlineRect(row.bounds().x(), row.bounds().y(), row.bounds().width(), row.bounds().height(), 1, BORDER);
            renderer.fillRect(row.bounds().x() + 12, row.bounds().y() + 15, 10, 10, module.enabled() ? ACTIVE : DANGER);
            text(renderer, module.name(), row.bounds().x() + 30, row.bounds().y() + 22, TEXT);
            text(renderer, module.enabled() ? "Enabled" : "Disabled", row.statusX(), row.bounds().y() + 22, module.enabled() ? ACTIVE : MUTED);

            for (int index = 0; index < settings.size(); index++) {
                Setting<?> setting = settings.get(index);
                Rectangle settingBounds = row.settingBounds(index);
                text(renderer, setting.name(), row.bounds().x() + 30, settingBounds.y() + 8, MUTED);
                text(renderer, SettingControls.displayValue(setting), row.valueX(), settingBounds.y() + 8, TEXT);
            }
            contentY = row.nextY();
        }
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        if (event.button() != MouseButton.LEFT) {
            return false;
        }

        PanelLayout layout = PanelLayout.from(bounds);

        for (ModuleCategory category : ModuleCategory.values()) {
            if (layout.categoryBounds(category).contains(event.x(), event.y())) {
                this.selectedCategory = category;
                this.rivet().recalculateNextFrame();
                return true;
            }
        }

        float contentY = layout.firstModuleY();
        for (Module module : this.modules.byCategory(this.selectedCategory)) {
            List<Setting<?>> settings = module.settings();
            ModuleRow row = layout.moduleRow(contentY, settings.size());
            if (row.toggleBounds().contains(event.x(), event.y())) {
                module.toggle();
                this.config.save();
                this.rivet().recalculateNextFrame();
                return true;
            }

            for (int index = 0; index < settings.size(); index++) {
                Setting<?> setting = settings.get(index);
                if (row.settingBounds(index).contains(event.x(), event.y()) && SettingControls.adjust(setting)) {
                    this.config.save();
                    this.rivet().recalculateNextFrame();
                    return true;
                }
            }
            contentY = row.nextY();
        }
        return true;
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return constraints;
    }

    private void text(final Renderer renderer, final String text, final float x, final float baselineY, final Color color) {
        renderer.text(this.rivet().backend().shapeText(text, color), x, baselineY, TextOrigin.Horizontal.LOGICAL_LEFT, TextOrigin.Vertical.BASELINE);
    }

    private record PanelLayout(float x, float y, float panelWidth, float panelHeight) {

        static PanelLayout from(final Rectangle bounds) {
            float panelWidth = Math.min(PANEL_MAX_WIDTH, bounds.width() - PANEL_MARGIN);
            float panelHeight = Math.min(PANEL_MAX_HEIGHT, bounds.height() - PANEL_MARGIN);
            return new PanelLayout((bounds.width() - panelWidth) / 2F, (bounds.height() - panelHeight) / 2F, panelWidth, panelHeight);
        }

        float contentDividerX() {
            return this.x + SIDEBAR_WIDTH;
        }

        float contentX() {
            return this.x + SIDEBAR_WIDTH + CONTENT_INSET;
        }

        float contentY() {
            return this.y + CONTENT_TOP;
        }

        float contentWidth() {
            return this.panelWidth - SIDEBAR_WIDTH - CONTENT_INSET * 2;
        }

        float firstModuleY() {
            return this.contentY() + 24;
        }

        Rectangle categoryBounds(final ModuleCategory category) {
            float categoryY = this.y + CATEGORY_TOP + category.ordinal() * CATEGORY_GAP;
            return new Rectangle(this.x + 8, categoryY - 4, SIDEBAR_WIDTH - 16, CATEGORY_HEIGHT);
        }

        ModuleRow moduleRow(final float y, final int settingCount) {
            float height = MODULE_BASE_HEIGHT + settingCount * SETTING_HEIGHT;
            return new ModuleRow(new Rectangle(this.contentX(), y, this.contentWidth(), height));
        }
    }

    private record ModuleRow(Rectangle bounds) {

        Rectangle toggleBounds() {
            return new Rectangle(this.bounds.x(), this.bounds.y(), this.bounds.width(), 34);
        }

        Rectangle settingBounds(final int index) {
            return new Rectangle(this.bounds.x() + 24, this.bounds.y() + 42 + index * SETTING_HEIGHT, this.bounds.width() - 24, 18);
        }

        float statusX() {
            return this.bounds.maxX() - 76;
        }

        float valueX() {
            return this.bounds.maxX() - 104;
        }

        float nextY() {
            return this.bounds.y() + this.bounds.height() + MODULE_GAP;
        }
    }
}
