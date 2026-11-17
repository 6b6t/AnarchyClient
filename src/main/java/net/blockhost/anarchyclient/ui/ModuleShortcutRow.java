package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.module.Module;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;
import net.lenni0451.rivet.text.model.TextOrigin;

import static net.blockhost.anarchyclient.ui.PanelStyle.DIVIDER;
import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BOX;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_WIDTH;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.iconNode;
import static net.blockhost.anarchyclient.ui.PanelWidgets.label;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.surface;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

final class ModuleShortcutRow extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final Module module;
    private final Surface divider = surface(DIVIDER);
    private final TextNode label;
    private final TextNode category;
    private final ToggleSwitch toggle;
    private final IconNode arrow = iconNode("chevron-right", FAINT);

    ModuleShortcutRow(final ModulePanel panel, final Module module) {
        super(gridLayout(0, 0));
        this.panel = panel;
        this.module = module;
        this.label = textNode(module.name(), () -> this.module.enabled() ? TEXT : MUTED);
        this.category = textNode(module.category().displayName(), () -> FAINT)
                .origin(TextOrigin.Horizontal.VISUAL_RIGHT, TextOrigin.Vertical.LOGICAL_CENTER);
        this.toggle = new ToggleSwitch(this.module::enabled, () -> this.panel.toggleModule(this.module));
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
            this.panel.selectModuleShortcut(this.module);
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