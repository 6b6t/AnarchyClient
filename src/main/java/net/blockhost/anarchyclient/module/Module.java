package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.setting.Setting;
import net.blockhost.anarchyclient.setting.SettingGroup;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

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

    protected final SelectSetting modeSetting(final String id, final String name, final ModuleMode defaultMode,
                                              final List<ModuleMode> modes) {
        return this.setting(SelectSetting.from(SelectSetting.builder()
                .id(id)
                .name(name)
                .defaultValue(defaultMode.name())
                .addAllOptions(modes.stream().map(ModuleMode::name).toList())
                .build()));
    }

    protected final void debugValue(final String key, final Object value) {
        DebugValueRegistry.put(this.id, key, value);
    }

    protected final void clearDebugValues() {
        DebugValueRegistry.clear(this.id);
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

    public Component chatMessage(final Minecraft client, final Component message) {
        return message;
    }

    public String sendChatMessage(final Minecraft client, final String message, final boolean command) {
        return message;
    }

    public Component tabPlayerName(final Minecraft client, final PlayerInfo playerInfo, final Component name) {
        return name;
    }

    public void itemTooltip(final Minecraft client, final ItemStack stack, final List<Component> lines) {
    }

    public void entityAdded(final Minecraft client, final Entity entity) {
    }

    public void entityRemoved(final Minecraft client, final Entity entity, final RemovalReason reason) {
    }

    public void blockBreakingProgress(final Minecraft client, final int breakerId, final BlockPos pos,
                                      final int progress) {
    }

    public boolean blockInteract(final Minecraft client, final InteractionHand hand, final BlockHitResult hitResult) {
        return false;
    }

    public boolean entityInteract(final Minecraft client, final Player player, final Entity entity,
                                  final EntityHitResult hitResult, final InteractionHand hand) {
        return false;
    }

    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        return false;
    }

    public void itemStopUse(final Minecraft client, final InteractionHand hand, final ItemStack stack,
                            final int remainingTicks) {
    }

    public boolean openScreen(final Minecraft client, final Screen screen) {
        return false;
    }

    public boolean mouseScroll(final Minecraft client, final double xOffset, final double yOffset) {
        return false;
    }

    public boolean particle(final Minecraft client, final ParticleOptions particle, final boolean alwaysShow) {
        return false;
    }

    public float fov(final Minecraft client, final float fov) {
        return fov;
    }

    public CameraTransform cameraTransform(final Minecraft client, final Vec3 position, final float yaw,
                                           final float pitch) {
        return new CameraTransform(position, yaw, pitch);
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

    public record CameraTransform(Vec3 position, float yaw, float pitch) {
    }
}
