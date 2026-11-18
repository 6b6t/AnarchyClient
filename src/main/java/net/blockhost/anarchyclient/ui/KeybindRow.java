package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

import static net.blockhost.anarchyclient.ui.PanelStyle.DIVIDER;
import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.SETTING_ROW_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.keyLabel;
import static net.blockhost.anarchyclient.ui.PanelWidgets.label;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.surface;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

/**
 * Module-level keybind control: shows the bound key on the right, click to listen for the next
 * key press. Actual capture happens in the screen (see {@link ModulePanel#handleKeybindCapture}).
 */
final class KeybindRow extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final Module module;
    private final Surface divider = surface(DIVIDER);
    private final TextNode label = textNode(Translations.text("anarchyclient.ui.keybind", "Keybind"), MUTED);
    private final TextNode description;
    private final TextNode value;

    KeybindRow(final ModulePanel panel, final Module module) {
        super(gridLayout(0, 0));
        this.panel = panel;
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
            this.panel.startKeybindListening(this.module);
            return true;
        }
        return false;
    }

    private String valueText() {
        return this.panel.isListeningFor(this.module) ? Translations.text("anarchyclient.ui.press_a_key", "Press a key...") : keyLabel(this.module.keybind().key());
    }

    private Color valueColor() {
        if (this.panel.isListeningFor(this.module)) {
            return GlassTheme.accent();
        }
        return this.module.keybind().bound() ? TEXT : FAINT;
    }

    private String descriptionText() {
        return this.panel.isListeningFor(this.module) ? Translations.text("anarchyclient.ui.press_a_key_esc_to_clear", "Press a key, Esc to clear") : Translations.text("anarchyclient.ui.click_to_bind_a_key", "Click to bind a key");
    }

    @Override
    public String layoutDebugLabel() {
        return "keybind";
    }
}