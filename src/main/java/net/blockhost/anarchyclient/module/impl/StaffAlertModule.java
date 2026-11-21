package net.blockhost.anarchyclient.module.impl;

import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.blockhost.anarchyclient.server.ServerProfileStore;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.target.TargetClassifier;
import net.blockhost.anarchyclient.util.MainThread;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class StaffAlertModule extends Module {

    private final ModuleManager modules;
    private final StringSetting staffNames = this.setting(StringSetting.from(StringSetting.builder()
            .id("staff_names")
            .name("Staff")
            .defaultValue("")
            .description("Extra staff names, separated by commas or spaces.")
            .build()));
    private final BooleanSetting disableModules = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("disable_modules")
            .name("Disable")
            .defaultValue(false)
            .build()));
    private final BooleanSetting proximity = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("proximity")
            .name("Proximity")
            .defaultValue(true)
            .build()));
    private final NumberSetting proximityRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("proximity_range")
            .name("Range")
            .defaultValue(80.0)
            .min(8.0)
            .max(256.0)
            .step(4.0)
            .build()));
    private final StringSetting moduleList = this.setting(StringSetting.from(StringSetting.builder()
            .id("modules")
            .name("Modules")
            .defaultValue("kill_aura, auto_clicker, velocity, glide, boost")
            .build()));
    /** Staff whose join was announced this session, by UUID so a leave can reset them. */
    private final Map<UUID, String> joinAlerted = new HashMap<>();
    /** Staff within proximity range last tick; a name alerts again after it leaves the range. */
    private Set<String> nearby = Set.of();

    public StaffAlertModule(final ModuleManager modules) {
        super(
                "staff_alert",
                "Staff Alert",
                ModuleCategory.MISC,
                "Warns in chat when a known or staff-like player joins or comes near, and can disable listed modules.");
        this.modules = modules;
    }

    @Override
    protected void onEnable() {
        this.reset();
    }

    @Override
    public void gameJoined(final Minecraft client, final ClientPacketListener listener) {
        this.reset();
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.reset();
    }

    private void reset() {
        this.joinAlerted.clear();
        this.nearby = Set.of();
    }

    @Override
    public void tick(final Minecraft client) {
        if (!this.proximity.value() || client.player == null || client.level == null) {
            return;
        }
        double rangeSqr = this.proximityRange.value() * this.proximityRange.value();
        Set<String> nearNow = new HashSet<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Player player
                    && player != client.player
                    && client.player.distanceToSqr(player) <= rangeSqr
                    && this.isStaff(player.getScoreboardName())) {
                String key = player.getScoreboardName().toLowerCase(Locale.ROOT);
                nearNow.add(key);
                if (!this.nearby.contains(key)) {
                    this.alert(client, player.getScoreboardName() + " nearby");
                }
            }
        }
        this.nearby = nearNow;
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (packet instanceof ClientboundPlayerInfoUpdatePacket info) {
            MainThread.run(() -> this.checkNewEntries(client, info));
        } else if (packet instanceof ClientboundPlayerInfoRemovePacket remove) {
            // A staff member who leaves and rejoins is announced again.
            MainThread.run(() -> remove.profileIds().forEach(this.joinAlerted::remove));
        }
        return false;
    }

    private void checkNewEntries(final Minecraft client, final ClientboundPlayerInfoUpdatePacket info) {
        if (!this.enabled() || client.player == null) {
            return;
        }
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : info.newEntries()) {
            GameProfile profile = entry.profile();
            if (profile != null
                    && this.isStaff(profile.name())
                    && this.joinAlerted.putIfAbsent(profile.id(), profile.name()) == null) {
                this.alert(client, profile.name());
            }
        }
    }

    private boolean isStaff(final String name) {
        String normalized = normalize(name);
        if (normalized.isBlank()) {
            return false;
        }
        Set<String> names = new LinkedHashSet<>(parseNames(this.staffNames.value()));
        names.addAll(parseNames(String.join(
                ",", ServerProfileStore.staffNames(ServerObserver.snapshot().rootDomain()))));
        return names.contains(normalized) || TargetClassifier.looksLikeStaffName(name);
    }

    private void alert(final Minecraft client, final String name) {
        if (client.player != null) {
            client.player.sendSystemMessage(Translations.component(
                    "anarchyclient.message.staff_alert.staff_detected", "Staff detected: %s.", name));
        }
        if (this.disableModules.value()) {
            for (String id : AutoDisableModule.parseModuleIds(this.moduleList.value())) {
                if (!id.equals(this.id())) {
                    this.modules.find(id).ifPresent(module -> module.enabled(false));
                }
            }
        }
    }

    static Set<String> parseNames(final String value) {
        Set<String> names = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return names;
        }
        for (String token : value.split("[,;|\\s]+")) {
            String name = normalize(token);
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
