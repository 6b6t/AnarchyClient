package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.blockhost.anarchyclient.server.ServerProfileStore;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

public final class AutoConfigModule extends Module {

    private final ModuleManager modules;
    private final BooleanSetting waitForDetection = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("wait_for_detection")
            .name("Wait Detect")
            .defaultValue(true)
            .build()));
    private final BooleanSetting notifyMissing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("notify_missing")
            .name("Missing")
            .defaultValue(false)
            .build()));
    private final BooleanSetting safetyDisable = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("safety_disable")
            .name("Safety")
            .defaultValue(true)
            .build()));
    private final NumberSetting safetyThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("safety_threshold")
            .name("Safety %")
            .defaultValue(45.0)
            .min(0.0)
            .max(100.0)
            .step(5.0)
            .build()));
    private final StringSetting riskyModules = this.setting(StringSetting.from(StringSetting.builder()
            .id("risky_modules")
            .name("Risky")
            .defaultValue("kill_aura, auto_clicker, velocity, glide, boost, blink, fake_lag, ping_spoof, scaffold, packet_mine")
            .build()));
    private final NumberSetting timeout = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("timeout")
            .name("Timeout")
            .defaultValue(80.0)
            .min(0.0)
            .max(400.0)
            .step(10.0)
            .build()));
    private String pendingDomain = "";
    private int pendingTicks;

    public AutoConfigModule(final ModuleManager modules) {
        super("auto_config", "Auto Config", ModuleCategory.MISC);
        this.modules = modules;
        this.enabled(true);
    }

    @Override
    public void gameJoined(final Minecraft client, final ClientPacketListener listener) {
        this.pendingDomain = ServerObserver.snapshot().rootDomain();
        this.pendingTicks = this.timeout.value().intValue();
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.clear();
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.pendingDomain.isBlank()) {
            return;
        }
        ServerObserver.Snapshot snapshot = ServerObserver.snapshot();
        if (this.waitForDetection.value() && snapshot.antiCheat().isEmpty() && this.pendingTicks-- > 0) {
            return;
        }
        this.applySafety(snapshot);
        this.apply(client, this.pendingDomain);
        this.clear();
    }

    @Override
    protected void onDisable() {
        this.clear();
    }

    private void apply(final Minecraft client, final String domain) {
        if (domain.isBlank()) {
            return;
        }
        if (ServerProfileStore.find(domain).isEmpty()) {
            if (this.notifyMissing.value() && client.player != null) {
                client.player.sendSystemMessage(Component.literal("No server profile for " + domain + "."));
            }
            return;
        }
        int changed = ServerProfileStore.apply(this.modules, domain);
        AnarchyClient.CONFIG.save();
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal("Applied server profile for " + domain + " (" + changed + " changes)."));
        }
    }

    private void applySafety(final ServerObserver.Snapshot snapshot) {
        if (!this.safetyDisable.value() || snapshot.safetyScore() < this.safetyThreshold.value()) {
            return;
        }
        for (String id : AutoDisableModule.parseModuleIds(this.riskyModules.value())) {
            if (!id.equals(this.id())) {
                this.modules.find(id).ifPresent(module -> module.enabled(false));
            }
        }
    }

    private void clear() {
        this.pendingDomain = "";
        this.pendingTicks = 0;
    }
}
