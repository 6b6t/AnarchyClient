package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.CARD;
import static net.blockhost.anarchyclient.ui.PanelStyle.CARD_HOVER;
import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BOX;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.fillCell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.iconButton;
import static net.blockhost.anarchyclient.ui.PanelWidgets.iconNode;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

import net.blockhost.anarchyclient.module.Module;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.input.mouse.MouseButton;
import net.lenni0451.rivet.input.mouse.MouseButtonEvent;
import net.lenni0451.rivet.input.mouse.MouseMoveEvent;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;

final class ModuleCard extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final Module module;
    private final Surface background;
    private final Surface hairline;
    private final IconNode icon;
    private final TextNode name;
    private final IconButton settingsButton;
    private final Anim hover = new Anim();
    private boolean hovered;

    ModuleCard(final ModulePanel panel, final Module module) {
        super(gridLayout(0, 0));
        this.panel = panel;
        this.module = module;
        this.background = new Surface(this::backgroundColor).cornerRadius(10F);
        this.hairline =
                new Surface(() -> this.module.enabled() ? GlassTheme.accent() : Color.TRANSPARENT).cornerRadius(1F);
        this.icon = iconNode(module::icon, () -> this.module.enabled() ? GlassTheme.accent() : MUTED);
        // The name is the card's primary content, so it stays at full contrast either way; state is
        // carried by the accent hairline, icon and background tint instead of by dimming the label.
        this.name = textNode(module.name(), () -> TEXT);
        this.settingsButton =
                iconButton("sliders", () -> this.inspected() ? GlassTheme.accent() : FAINT, this::toggleInspect);
        place(this.background, fillCell(0, 0, 4, 1));
        place(
                this.hairline,
                cell(0, 0, 4, 1, 0F, 0F, GridAnchor.TOP, GridFill.HORIZONTAL, padding(10F, 0F, 10F, 0F), null, 2F));
        place(
                this.icon,
                cell(
                        0,
                        0,
                        1,
                        1,
                        0F,
                        0F,
                        GridAnchor.CENTER,
                        GridFill.NONE,
                        padding(7F, 0F, 0F, 0F),
                        ICON_BOX,
                        ICON_BOX));
        place(
                this.name,
                cell(1, 0, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH, padding(7F, 0F, 4F, 0F), null, null));
        place(
                this.settingsButton,
                cell(3, 0, 1, 1, 0F, 0F, GridAnchor.CENTER, GridFill.NONE, padding(0F, 0F, 2F, 0F), 24F, 24F));
        this.addChild(this.background);
        this.addChild(this.hairline);
        this.addChild(this.icon);
        this.addChild(this.name);
        this.addChild(this.settingsButton);
    }

    private void toggleInspect() {
        if (this.inspected()) {
            this.panel.closeInspector();
        } else {
            this.panel.inspectModule(this.module);
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
        this.panel.hoveredModule = this.module;
    }

    @Override
    protected void onComponentMouseLeave() {
        this.hovered = false;
        if (this.panel.hoveredModule == this.module) {
            this.panel.hoveredModule = null;
        }
        super.onComponentMouseLeave();
    }

    @Override
    protected boolean onComponentMouseDown(final MouseButtonEvent event, final Size size) {
        if (super.onComponentMouseDown(event, size)) {
            return true;
        }
        if (event.button() == MouseButton.LEFT) {
            this.panel.toggleModule(this.module);
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
        return new Size(constraints.width(), this.panel.modulesPanel.cards.cardHeight());
    }

    @Override
    public String layoutDebugLabel() {
        return this.module.id();
    }

    private boolean inspected() {
        return this.module == this.panel.inspectedModule;
    }

    private Color backgroundColor() {
        float glow = this.hover.toward(this.hovered ? 1F : 0F, 14F);
        Color base = this.module.enabled() ? GlassTheme.accentSoft().multiplyAlpha(0.55F) : CARD;
        Color hoverColor = this.module.enabled() ? GlassTheme.accentSoft() : CARD_HOVER;
        return Color.interpolate(glow, base, hoverColor);
    }
}
