package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.blockhost.anarchyclient.server.ServerProfileStore;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

public final class AntiCheatDetectModule extends Module {

    private final BooleanSetting storeProfile = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("store_profile")
            .name("Store Profile")
            .defaultValue(true)
            .build()));
    private final BooleanSetting showUnknown = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("show_unknown")
            .name("Unknown")
            .defaultValue(false)
            .build()));
    private String lastAlert = "";

    public AntiCheatDetectModule() {
        super("anti_cheat_detect", "AntiCheat Detect", ModuleCategory.MISC, "Looks for anti-cheat signals and exposes them to modules that need stricter timing or safer behavior.");
    }

    @Override
    protected void onEnable() {
        this.lastAlert = "";
    }

    @Override
    public void tick(final Minecraft client) {
        ServerObserver.Snapshot snapshot = ServerObserver.snapshot();
        String antiCheat = snapshot.antiCheat().orElse("");
        // Keyed by server too, so joining another server with the same anti-cheat still alerts and records it.
        String key = snapshot.rootDomain() + "|" + antiCheat;
        if (antiCheat.isBlank() || key.equals(this.lastAlert) || "Unknown".equals(antiCheat) && !this.showUnknown.value()) {
            return;
        }
        this.lastAlert = key;
        if (client.player != null) {
            String labels = snapshot.environmentLabels().isEmpty() ? "" : " " + snapshot.environmentLabels();
            client.player.sendSystemMessage(Translations.component("anarchyclient.message.anti_cheat_detect.detected_anti_cheat_safety_100", "Detected anti-cheat: %s (safety %s/100).%s", antiCheat, snapshot.safetyScore(), labels));
        }
        if (this.storeProfile.value()) {
            ServerProfileStore.recordAntiCheat(snapshot.rootDomain(), antiCheat);
        }
    }
}
