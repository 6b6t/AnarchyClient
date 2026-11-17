package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.SettingGroup;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.component.container.ScrollContainer;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.layout.list.VerticalListLayout;
import net.lenni0451.rivet.math.Padding;
import net.lenni0451.rivet.math.Size;

import java.util.List;

import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.fillCell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.settingRow;
import static net.blockhost.anarchyclient.ui.PanelWidgets.showGroupHeaders;
import static net.blockhost.anarchyclient.ui.PanelWidgets.visibleSettingGroups;

final class ModuleInspector extends Container implements LayoutDebugLabel {

    private final ModulePanel panel;

    private final GlassSurface background;
    private final InspectorHeader header;
    private final Container settings;
    private final ScrollContainer scroll;

    ModuleInspector(final ModulePanel panel) {
        super(gridLayout(0, 0));
        this.panel = panel;
        this.background = this.panel.glassDeepPanel();
        this.header = new InspectorHeader(this.panel);
        this.settings = new Container(new VerticalListLayout(0, true));
        this.scroll = new LayoutDebugScrollContainer(this.settings);
        this.panel.configureScroll(this.scroll);
        place(this.background, fillCell(0, 0, 1, 2));
        // No fixed height: the header grows when the module description folds onto a second line.
        place(this.header, cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                Padding.EMPTY, null, null));
        place(this.scroll, cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(0F, 0F, 0F, PADDING), null, null));
        this.addChild(this.background);
        this.addChild(this.header);
        this.addChild(this.scroll);
    }

    void refresh() {
        this.settings.clearChildren();
        Module module = this.panel.inspectedModule;
        if (module == null) {
            this.settings.addChild(new EmptyState("Open a module to edit its settings."));
        } else {
            this.settings.addChild(new KeybindRow(this.panel, module));
            List<SettingGroup> groups = visibleSettingGroups(module);
            boolean groupHeaders = showGroupHeaders(groups);
            for (SettingGroup group : groups) {
                if (groupHeaders) {
                    this.settings.addChild(new GroupHeader(group.name()));
                }
                for (Setting<?> setting : group.settings()) {
                    if (setting.visible()) {
                        this.settings.addChild(settingRow(setting, this::refreshAfterSettingChange, this.panel.config::markDirty));
                    }
                }
            }
            if (this.settings.children().isEmpty()) {
                this.settings.addChild(new EmptyState("This module has no visible settings."));
            }
        }
        this.panel.requestFrame();
    }

    private void refreshAfterSettingChange() {
        this.panel.config.save();
        this.refresh();
        this.panel.refreshModuleList();
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return constraints;
    }

    @Override
    public String layoutDebugLabel() {
        return this.panel.inspectedModule == null ? "empty" : this.panel.inspectedModule.id();
    }
}