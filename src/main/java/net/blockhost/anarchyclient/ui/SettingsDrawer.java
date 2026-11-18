package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.notification.ToggleNotifications;
import net.lenni0451.rivet.component.Component;
import net.lenni0451.rivet.component.container.Container;
import net.lenni0451.rivet.layout.grid.GridAnchor;
import net.lenni0451.rivet.layout.grid.GridFill;
import net.lenni0451.rivet.layout.list.VerticalListLayout;
import net.lenni0451.rivet.math.Size;

import java.util.Locale;

import static net.blockhost.anarchyclient.ui.PanelStyle.DIVIDER;
import static net.blockhost.anarchyclient.ui.PanelStyle.FAINT;
import static net.blockhost.anarchyclient.ui.PanelStyle.HEADER_HEIGHT;
import static net.blockhost.anarchyclient.ui.PanelStyle.ICON_BUTTON_SIZE;
import static net.blockhost.anarchyclient.ui.PanelStyle.MUTED;
import static net.blockhost.anarchyclient.ui.PanelStyle.PADDING;
import static net.blockhost.anarchyclient.ui.PanelStyle.TEXT;
import static net.blockhost.anarchyclient.ui.PanelWidgets.cell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.fillCell;
import static net.blockhost.anarchyclient.ui.PanelWidgets.gridLayout;
import static net.blockhost.anarchyclient.ui.PanelWidgets.iconButton;
import static net.blockhost.anarchyclient.ui.PanelWidgets.padding;
import static net.blockhost.anarchyclient.ui.PanelWidgets.place;
import static net.blockhost.anarchyclient.ui.PanelWidgets.surface;
import static net.blockhost.anarchyclient.ui.PanelWidgets.textNode;

final class SettingsDrawer extends Container implements LayoutDebugLabel {

    private static final float HEADER_HEIGHT = 38F;
    static final float ROW_HEIGHT = 36F;

    private final ModulePanel panel;

    private final GlassSurface background;
    private final Surface headerDivider;
    private final IconButton backIcon;
    private final TextNode title;
    private final IconButton closeIcon;
    private final Container body;

    SettingsDrawer(final ModulePanel panel) {
        super(gridLayout(0, 4));
        this.panel = panel;
        this.background = this.panel.glassDeepPanel();
        this.headerDivider = surface(DIVIDER);
        this.backIcon = iconButton("chevron-left", () -> MUTED,
                this.panel::openRootDrawer,
                () -> this.panel.drawer != Drawer.ROOT && this.panel.drawer != Drawer.NONE);
        this.title = textNode(this::title, () -> TEXT);
        this.closeIcon = iconButton("x", () -> FAINT, () -> {
            this.panel.drawer = Drawer.NONE;
            this.panel.requestFrame();
        });
        this.body = new Container(new VerticalListLayout(0, true));
        place(this.background, fillCell(0, 0, 1, 2));
        place(this.headerDivider, cell(0, 0, 1, 1, 0F, 0F, GridAnchor.BOTTOM, GridFill.HORIZONTAL,
                padding(PADDING, 0F, PADDING, 0F), null, 1F));
        place(this.backIcon, cell(0, 0, 1, 1, 0F, 0F, GridAnchor.LEFT, GridFill.NONE,
                padding(6F, 0F, 0F, 0F), ICON_BUTTON_SIZE, ICON_BUTTON_SIZE));
        place(this.title, cell(0, 0, 1, 1, 1F, 0F, GridAnchor.CENTER, GridFill.BOTH,
                padding(38F, 0F, 38F, 0F), null, HEADER_HEIGHT));
        place(this.closeIcon, cell(0, 0, 1, 1, 0F, 0F, GridAnchor.RIGHT, GridFill.NONE,
                padding(0F, 0F, 6F, 0F), ICON_BUTTON_SIZE, ICON_BUTTON_SIZE));
        place(this.body, cell(0, 1, 1, 1, 1F, 1F, GridAnchor.CENTER, GridFill.BOTH,
                padding(0F, 0F, 0F, PADDING), null, null));
        this.addChild(this.background);
        this.addChild(this.headerDivider);
        this.addChild(this.backIcon);
        this.addChild(this.title);
        this.addChild(this.closeIcon);
        this.addChild(this.body);
    }

    void refresh() {
        this.body.clearChildren();
        switch (this.panel.drawer) {
            case ROOT -> this.buildRoot();
            case GENERAL -> this.buildGeneral();
            case MODULES -> this.buildModules();
            case SOUND -> this.buildSound();
            case NOTIFICATIONS -> this.buildNotifications();
            case NONE -> {
            }
        }
        this.panel.requestFrame();
    }

    @Override
    public Size computeIdealSize(final Size constraints) {
        return constraints;
    }

    @Override
    public String layoutDebugLabel() {
        return this.panel.drawer.name().toLowerCase(Locale.ROOT);
    }

    private String title() {
        return switch (this.panel.drawer) {
            case ROOT -> Translations.text("anarchyclient.ui.settings", "Settings");
            case GENERAL -> Translations.text("anarchyclient.ui.general", "General");
            case MODULES -> Translations.text("anarchyclient.ui.modules", "Modules");
            case SOUND -> Translations.text("anarchyclient.ui.sound", "Sound");
            case NOTIFICATIONS -> Translations.text("anarchyclient.ui.notifications", "Notifications");
            case NONE -> "";
        };
    }

    private void buildRoot() {
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.general", "General"), true, null, () -> "", () -> this.openNestedDrawer(Drawer.GENERAL), this::refresh));
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.modules", "Modules"), true, null, () -> "", () -> this.openNestedDrawer(Drawer.MODULES), this::refresh));
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.sound", "Sound"), true, null, () -> "", () -> this.openNestedDrawer(Drawer.SOUND), this::refresh));
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.notifications", "Notifications"), true, null, () -> "", () -> this.openNestedDrawer(Drawer.NOTIFICATIONS), this::refresh));
    }

    private void buildGeneral() {
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.save_config_now", "Save config now"), false, null, () -> "", this.panel::saveConfigNow, this::refresh));
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.reload_config", "Reload config"), false, null, () -> "", this.panel::reloadConfig, this::refresh));
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.reset_ui_preferences", "Reset UI preferences"), false, null, () -> "", this.panel::resetUiPreferences, this::refresh));
        this.addRow(new OptionRow(this.panel, "Dump layout tree", false, null, () -> "", this.panel::dumpLayoutTree, this::refresh));
    }

    private void buildModules() {
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.show_disabled_modules", "Show disabled modules"), false,
                () -> this.panel.showDisabledModules,
                () -> {
                    this.panel.showDisabledModules = !this.panel.showDisabledModules;
                    this.panel.saveUiPreferences();
                    this.panel.refreshModuleList();
                }, this::refresh));
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.enabled_modules_first", "Enabled modules first"), false,
                () -> this.panel.enabledFirst,
                () -> {
                    this.panel.enabledFirst = !this.panel.enabledFirst;
                    this.panel.saveUiPreferences();
                    this.panel.refreshModuleList();
                }, this::refresh));
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.reset_module_filters", "Reset module filters"), false, null, () -> "",
                this.panel::resetModuleListPreferences, this::refresh));
    }

    private void buildSound() {
        int rows = this.addShortcuts(
                "sound_locator",
                "sound_blocker",
                "notebot",
                "auto_fish",
                "ambience"
        );
        if (rows == 0) {
            this.addBody(new EmptyState(Translations.text("anarchyclient.ui.no_sound_modules_are_registered", "No sound modules are registered.")));
        }
    }

    private void buildNotifications() {
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.toggle_notifications", "Toggle notifications"), false,
                ToggleNotifications::enabled,
                () -> {
                    ToggleNotifications.enabled(!ToggleNotifications.enabled());
                    this.panel.config.save();
                }, this::refresh));
        this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.style", "Style"), false, null,
                () -> ToggleNotifications.mode().displayName(),
                () -> {
                    ToggleNotifications.mode(ToggleNotifications.mode().next());
                    this.panel.config.save();
                }, this::refresh));
        if (ToggleNotifications.mode() == ToggleNotifications.Mode.POPUP) {
            this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.position", "Position"), false, null,
                    () -> ToggleNotifications.corner().displayName(),
                    () -> {
                        ToggleNotifications.corner(ToggleNotifications.corner().next());
                        this.panel.config.save();
                    }, this::refresh));
            this.addRow(new OptionRow(this.panel, Translations.text("anarchyclient.ui.duration", "Duration"), false, null,
                    () -> (ToggleNotifications.durationMs() / 1000) + "s",
                    () -> {
                        ToggleNotifications.cycleDuration();
                        this.panel.config.save();
                    }, this::refresh));
        }
        this.addShortcuts(
                "notifier",
                "gamemode_notifier",
                "lag_notifier_hud",
                "staff_alert",
                "auto_reconnect"
        );
    }

    private void addRow(final OptionRow row) {
        this.addBody(row);
    }

    private void addBody(final Component component) {
        this.body.addChild(component);
    }

    private void openNestedDrawer(final Drawer drawer) {
        this.panel.drawer = drawer;
        this.refresh();
    }

    private int addShortcuts(final String... moduleIds) {
        int rows = 0;
        for (String moduleId : moduleIds) {
            if (this.panel.addModuleShortcut(this.body, moduleId)) {
                rows++;
            }
        }
        return rows;
    }
}