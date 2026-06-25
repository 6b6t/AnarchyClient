package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class AutoDisableModule extends Module {

    private final ModuleManager modules;
    private final StringSetting moduleList = this.setting(StringSetting.from(StringSetting.builder()
            .id("modules")
            .name("Modules")
            .defaultValue("kill_aura, auto_clicker, velocity, fly, glide, boost")
            .build()));
    private final BooleanSetting onDeath = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("on_death")
            .name("Death")
            .defaultValue(true)
            .build()));
    private final BooleanSetting onDisconnect = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("on_disconnect")
            .name("Disconnect")
            .defaultValue(true)
            .build()));
    private boolean wasDead;

    public AutoDisableModule(final ModuleManager modules) {
        super("auto_disable", "Auto Disable", ModuleCategory.MISC);
        this.modules = modules;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null) {
            this.wasDead = false;
            return;
        }
        boolean dead = player.isDeadOrDying();
        if (dead && !this.wasDead && this.onDeath.value()) {
            this.disableConfigured();
        }
        this.wasDead = dead;
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.wasDead = false;
        if (this.onDisconnect.value()) {
            this.disableConfigured();
        }
    }

    private void disableConfigured() {
        for (String id : parseModuleIds(this.moduleList.value())) {
            if (id.equals(this.id())) {
                continue;
            }
            this.modules.find(id).ifPresent(module -> module.enabled(false));
        }
    }

    static Set<String> parseModuleIds(final String value) {
        Set<String> ids = new LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return ids;
        }
        for (String token : value.split("[,;|\\s]+")) {
            String id = token.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return ids;
    }
}
