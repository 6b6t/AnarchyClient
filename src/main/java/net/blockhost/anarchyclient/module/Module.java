package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {

    private final String id;
    private final String name;
    private final ModuleCategory category;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Setting<?>> settingsView = Collections.unmodifiableList(this.settings);
    private boolean enabled;

    protected Module(final String id, final String name, final ModuleCategory category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }

    public final String id() {
        return this.id;
    }

    public final String name() {
        return this.name;
    }

    public final ModuleCategory category() {
        return this.category;
    }

    public final boolean enabled() {
        return this.enabled;
    }

    public final void enabled(final boolean enabled) {
        if (this.enabled == enabled) {
            return;
        }
        this.enabled = enabled;
        if (enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }

    public final void toggle() {
        this.enabled(!this.enabled);
    }

    public final List<Setting<?>> settings() {
        return this.settingsView;
    }

    protected final <T extends Setting<?>> T setting(final T setting) {
        this.settings.add(setting);
        return setting;
    }

    public void tick(final Minecraft client) {
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }
}
