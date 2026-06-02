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
import net.lenni0451.rivet.math.Rectangle;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

import java.util.List;

public final class ModulePanel extends Component {

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
        renderer.fillRect(0, 0, bounds.width(), bounds.height(), BACKDROP);

        float panelWidth = Math.min(760, bounds.width() - 48);
        float panelHeight = Math.min(480, bounds.height() - 48);
        float x = (bounds.width() - panelWidth) / 2F;
        float y = (bounds.height() - panelHeight) / 2F;
        float sidebarWidth = 156;

        renderer.fillRect(x, y, panelWidth, panelHeight, PANEL);
        renderer.outlineRect(x, y, panelWidth, panelHeight, 1, BORDER);
        renderer.fillRect(x, y, sidebarWidth, panelHeight, PANEL_DARK);
        renderer.line(x + sidebarWidth, y, x + sidebarWidth, y + panelHeight, 1, BORDER);

        text(renderer, "AnarchyClient", x + 16, y + 24, TEXT);
        text(renderer, "Fabric 26.1.2", x + 16, y + 42, MUTED);

        float categoryY = y + 76;
        for (ModuleCategory category : ModuleCategory.values()) {
            boolean selected = this.selectedCategory == category;
            if (selected) {
                renderer.fillRect(x + 8, categoryY - 4, sidebarWidth - 16, 24, Color.fromRGB(40, 40, 44));
                renderer.fillRect(x + 8, categoryY - 4, 3, 24, ACTIVE);
            }
            text(renderer, category.displayName(), x + 18, categoryY + 10, selected ? TEXT : MUTED);
            categoryY += 30;
        }

        float contentX = x + sidebarWidth + 18;
        float contentY = y + 24;
        text(renderer, this.selectedCategory.displayName(), contentX, contentY, TEXT);
        contentY += 24;

        List<Module> visibleModules = this.modules.byCategory(this.selectedCategory);
        for (Module module : visibleModules) {
            float rowHeight = 48 + module.settings().size() * 22;
            renderer.fillRect(contentX, contentY, panelWidth - sidebarWidth - 36, rowHeight, PANEL_DARK);
            renderer.outlineRect(contentX, contentY, panelWidth - sidebarWidth - 36, rowHeight, 1, BORDER);
            renderer.fillRect(contentX + 12, contentY + 15, 10, 10, module.enabled() ? ACTIVE : DANGER);
            text(renderer, module.name(), contentX + 30, contentY + 22, TEXT);
            text(renderer, module.enabled() ? "Enabled" : "Disabled", contentX + panelWidth - sidebarWidth - 112, contentY + 22, module.enabled() ? ACTIVE : MUTED);

            float settingY = contentY + 42;
            for (Setting<?> setting : module.settings()) {
                text(renderer, setting.name(), contentX + 30, settingY + 8, MUTED);
                text(renderer, displayValue(setting), contentX + panelWidth - sidebarWidth - 140, settingY + 8, TEXT);
                settingY += 22;
            }
            contentY += rowHeight + 10;
        }
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Rectangle bounds) {
        if (event.button() != MouseButton.LEFT) {
            return false;
        }

        float panelWidth = Math.min(760, bounds.width() - 48);
        float panelHeight = Math.min(480, bounds.height() - 48);
        float x = (bounds.width() - panelWidth) / 2F;
        float y = (bounds.height() - panelHeight) / 2F;
        float sidebarWidth = 156;

        float categoryY = y + 76;
        for (ModuleCategory category : ModuleCategory.values()) {
            if (new Rectangle(x + 8, categoryY - 4, sidebarWidth - 16, 24).contains(event.x(), event.y())) {
                this.selectedCategory = category;
                this.rivet().recalculateNextFrame();
                return true;
            }
            categoryY += 30;
        }

        float contentX = x + sidebarWidth + 18;
        float contentY = y + 48;
        for (Module module : this.modules.byCategory(this.selectedCategory)) {
            float rowHeight = 48 + module.settings().size() * 22;
            Rectangle moduleRow = new Rectangle(contentX, contentY, panelWidth - sidebarWidth - 36, 34);
            if (moduleRow.contains(event.x(), event.y())) {
                module.toggle();
                this.config.save();
                this.rivet().recalculateNextFrame();
                return true;
            }

            float settingY = contentY + 42;
            for (Setting<?> setting : module.settings()) {
                Rectangle settingRow = new Rectangle(contentX + 24, settingY, panelWidth - sidebarWidth - 60, 18);
                if (settingRow.contains(event.x(), event.y())) {
                    adjustSetting(setting);
                    this.config.save();
                    this.rivet().recalculateNextFrame();
                    return true;
                }
                settingY += 22;
            }
            contentY += rowHeight + 10;
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

    private static String displayValue(final Setting<?> setting) {
        Object value = setting.value();
        if (value instanceof Double number) {
            return number % 1 == 0 ? Integer.toString(number.intValue()) : String.format("%.1f", number);
        }
        return String.valueOf(value);
    }

    private static void adjustSetting(final Setting<?> setting) {
        if (setting instanceof BooleanSetting bool) {
            bool.value(!bool.value());
        } else if (setting instanceof NumberSetting number) {
            double next = number.value() + number.step();
            if (next > number.max()) {
                next = number.min();
            }
            number.value(next);
        }
    }
}
