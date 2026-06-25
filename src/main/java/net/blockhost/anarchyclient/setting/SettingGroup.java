package net.blockhost.anarchyclient.setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SettingGroup {

    private final String id;
    private final String name;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Setting<?>> settingsView = Collections.unmodifiableList(this.settings);

    public SettingGroup(final String id, final String name) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Setting group id must not be blank");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Setting group name must not be blank");
        }
        this.id = id;
        this.name = name;
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public List<Setting<?>> settings() {
        return this.settingsView;
    }

    public boolean visible() {
        for (Setting<?> setting : this.settings) {
            if (setting.visible()) {
                return true;
            }
        }
        return false;
    }

    public <T extends Setting<?>> T add(final T setting) {
        this.settings.add(setting);
        return setting;
    }
}
