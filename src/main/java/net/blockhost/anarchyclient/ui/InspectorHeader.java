package net.blockhost.anarchyclient.ui;

import static net.blockhost.anarchyclient.ui.PanelStyle.DIVIDER;
import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BOX;
import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BUTTON_SIZE;
import static net.blockhost.anarchyclient.ui.PanelStyle.INSPECTOR_HEADER_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.SWITCH_WIDTH;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelStyle.WARNING;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.fixedCell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.iconButton;
import static net.blockhost.anarchyclient.ui.PanelWidgets.iconNode;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.surface;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.lenni0451.commons.color.Color;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.math.Size;

final class InspectorHeader extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final TextNode description;

    InspectorHeader(final ModulePanel panel) {
        super(gridLayout(0, 0));
        this.panel = panel;
        Surface divider = surface(DIVIDER);
        IconNode moduleIcon = iconNode(this::moduleIcon, this::moduleIconColor);
        TextNode title = textNode(
                () -> this.panel.inspectedModule == null
                        ? Translations.text("anarchyclient.ui.no_module", "No module")
                        : this.panel.inspectedModule.name(),
                () -> this.panel.inspectedModule == null ? MUTED : TEXT);
        TextNode category = textNode(
                () -> this.panel.inspectedModule == null
                        ? ""
                        : this.panel.inspectedModule.category().displayName(),
                () -> FAINT);
        this.description = textNode(
                        () -> this.panel.inspectedModule == null ? "" : this.panel.inspectedModule.description(),
                        () -> FAINT)
                .maxLines(Integer.MAX_VALUE);
        IconButton star = iconButton("star", this::starColor, () -> {
            if (this.panel.inspectedModule != null) {
                this.panel.toggleFavorite(this.panel.inspectedModule);
            }
        });
        ToggleSwitch toggle = new ToggleSwitch(
                () -> this.panel.inspectedModule != null,
                () -> this.panel.inspectedModule != null && this.panel.inspectedModule.enabled(),
                () -> {
                    if (this.panel.inspectedModule != null) {
                        this.panel.toggleModule(this.panel.inspectedModule);
                    }
                });
        IconButton close = iconButton("x", () -> FAINT, this.panel::closeInspector);
        place(
                divider,
                cell(
                        0,
                        3,
                        5,
                        1,
                        0F,
                        0F,
                        GridAnchor.BOTTOM,
                        GridFill.HORIZONTAL,
                        padding(PADDING, 0F, PADDING, 0F),
                        null,
                        1F));
        place(
                moduleIcon,
                cell(
                        0,
                        0,
                        1,
                        3,
                        0F,
                        0F,
                        GridAnchor.CENTER,
                        GridFill.NONE,
                        padding(PADDING, 0F, 0F, 0F),
                        ICON_BOX,
                        ICON_BOX));
        place(title, cell(1, 0, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH, padding(8F, 10F, 8F, 0F), null, 20F));
        place(category, cell(1, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH, padding(8F, 0F, 8F, 0F), null, 16F));
        place(
                this.description,
                cell(1, 2, 2, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH, padding(8F, 0F, 8F, 8F), null, null));
        place(star, fixedCell(2, 0, 1, 2, ICON_BUTTON_SIZE, ICON_BUTTON_SIZE, GridAnchor.CENTER));
        place(
                toggle,
                cell(
                        3,
                        0,
                        1,
                        2,
                        0F,
                        0F,
                        GridAnchor.CENTER,
                        GridFill.NONE,
                        padding(0F, 0F, 4F, 0F),
                        SWITCH_WIDTH,
                        SWITCH_HEIGHT));
        place(
                close,
                cell(
                        4,
                        0,
                        1,
                        2,
                        0F,
                        0F,
                        GridAnchor.CENTER,
                        GridFill.NONE,
                        padding(0F, 0F, 6F, 0F),
                        ICON_BUTTON_SIZE,
                        ICON_BUTTON_SIZE));
        this.addChild(divider);
        this.addChild(moduleIcon);
        this.addChild(title);
        this.addChild(category);
        this.addChild(this.description);
        this.addChild(star);
        this.addChild(toggle);
        this.addChild(close);
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        float descriptionWidth = Math.max(0F, constraints.width() - PADDING - ICON_BOX - 8F - 16F);
        float extra = (this.description.lineCount(descriptionWidth) - 1) * 11F;
        return new Size(constraints.width(), INSPECTOR_HEADER_HEIGHT + extra);
    }

    @Override
    public String layoutDebugLabel() {
        return this.panel.inspectedModule == null ? "empty" : this.panel.inspectedModule.id();
    }

    private String moduleIcon() {
        Module module = this.panel.inspectedModule;
        return module == null ? "" : module.icon();
    }

    private Color moduleIconColor() {
        Module module = this.panel.inspectedModule;
        if (module == null) {
            return Color.TRANSPARENT;
        }
        return module.enabled() ? GlassTheme.accent() : MUTED;
    }

    private Color starColor() {
        Module module = this.panel.inspectedModule;
        if (module == null) {
            return Color.TRANSPARENT;
        }
        return this.panel.config.moduleFavorite(module.id()) ? WARNING : FAINT;
    }
}
