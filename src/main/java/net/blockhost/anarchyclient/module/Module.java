package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.SettingGroup;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {

    private final String id;
    private final String name;
    private final ModuleCategory category;
    private final List<String> aliases;
    private final ModuleKeybind keybind;
    private final List<Setting<?>> settings = new ArrayList<>();
    private final List<Setting<?>> settingsView = Collections.unmodifiableList(this.settings);
    private final List<SettingGroup> settingGroups = new ArrayList<>();
    private final List<SettingGroup> settingGroupsView = Collections.unmodifiableList(this.settingGroups);
    private final SettingGroup defaultSettingGroup = new SettingGroup("general", "General");
    private ActivationListener activationListener;
    private boolean enabled;

    protected Module(final String id, final String name, final ModuleCategory category) {
        this(id, name, category, List.of());
    }

    protected Module(final String id, final String name, final ModuleCategory category, final List<String> aliases) {
        this(id, name, category, aliases, ModuleKeybind.unbound());
    }

    protected Module(final String id, final String name, final ModuleCategory category, final List<String> aliases,
                     final ModuleKeybind keybind) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.aliases = List.copyOf(aliases);
        this.keybind = keybind == null ? ModuleKeybind.unbound() : keybind;
        this.settingGroups.add(this.defaultSettingGroup);
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

    public final List<String> aliases() {
        return this.aliases;
    }

    public final ModuleKeybind keybind() {
        return this.keybind;
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
        if (this.activationListener != null) {
            this.activationListener.onActivationChanged(this, this.enabled);
        }
    }

    public final void toggle() {
        this.enabled(!this.enabled);
    }

    public final List<Setting<?>> settings() {
        return this.settingsView;
    }

    public final List<SettingGroup> settingGroups() {
        return this.settingGroupsView;
    }

    protected final <T extends Setting<?>> T setting(final T setting) {
        return this.setting(this.defaultSettingGroup, setting);
    }

    protected final SettingGroup settingGroup(final String id, final String name) {
        for (SettingGroup group : this.settingGroups) {
            if (group.id().equals(id)) {
                return group;
            }
        }
        SettingGroup group = new SettingGroup(id, name);
        this.settingGroups.add(group);
        return group;
    }

    protected final <T extends Setting<?>> T setting(final SettingGroup group, final T setting) {
        if (!this.settingGroups.contains(group)) {
            this.settingGroups.add(group);
        }
        group.add(setting);
        this.settings.add(setting);
        return setting;
    }

    final void activationListener(final ActivationListener activationListener) {
        this.activationListener = activationListener;
    }

    public void tick(final Minecraft client) {
    }

    public void updateInput(final Minecraft client, final ClientInput input) {
    }

    public boolean preventEdgeFall(final Minecraft client, final Player player) {
        return false;
    }

    public boolean mouseClick(final Minecraft client, final MouseButtonInfo buttonInfo, final int action) {
        return false;
    }

    public boolean attackEntity(final Minecraft client, final Player player, final Entity target) {
        return false;
    }

    public void renderWorld(final LevelRenderContext context) {
    }

    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
    }

    public void soundPacket(final Minecraft client, final ClientboundSoundPacket packet) {
    }

    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return false;
    }

    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return packet;
    }

    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return false;
    }

    public void sentPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
    }

    public void gameJoined(final Minecraft client, final ClientPacketListener listener) {
    }

    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }

    interface ActivationListener {

        void onActivationChanged(Module module, boolean enabled);
    }
}
