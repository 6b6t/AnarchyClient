package net.blockhost.anarchyclient.module.impl;

import com.mojang.authlib.GameProfile;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.blockhost.anarchyclient.server.ServerProfileStore;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.target.TargetClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

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
    private final Set<String> alerted = new LinkedHashSet<>();

    public StaffAlertModule(final ModuleManager modules) {
        super("staff_alert", "Staff Alert", ModuleCategory.MISC);
        this.modules = modules;
    }

    @Override
    protected void onEnable() {
        this.alerted.clear();
    }

    @Override
    public void tick(final Minecraft client) {
        if (!this.proximity.value() || client.player == null || client.level == null) {
            return;
        }
        double rangeSqr = this.proximityRange.value() * this.proximityRange.value();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof Player player && player != client.player
                    && client.player.distanceToSqr(player) <= rangeSqr
                    && this.isStaff(player.getScoreboardName())
                    && this.alerted.add(("near:" + player.getScoreboardName()).toLowerCase(Locale.ROOT))) {
                this.alert(client, player.getScoreboardName() + " nearby");
            }
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.player == null || !(packet instanceof ClientboundPlayerInfoUpdatePacket info)) {
            return false;
        }
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : info.newEntries()) {
            GameProfile profile = entry.profile();
            if (profile != null && this.isStaff(profile.name()) && this.alerted.add(profile.name().toLowerCase(Locale.ROOT))) {
                this.alert(client, profile.name());
            }
        }
        return false;
    }

    private boolean isStaff(final String name) {
        String normalized = normalize(name);
        if (normalized.isBlank()) {
            return false;
        }
        Set<String> names = new LinkedHashSet<>(parseNames(this.staffNames.value()));
        names.addAll(parseNames(String.join(",", ServerProfileStore.staffNames(ServerObserver.snapshot().rootDomain()))));
        return names.contains(normalized) || TargetClassifier.looksLikeStaffName(name);
    }

    private void alert(final Minecraft client, final String name) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("Staff detected: " + name + "."));
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
