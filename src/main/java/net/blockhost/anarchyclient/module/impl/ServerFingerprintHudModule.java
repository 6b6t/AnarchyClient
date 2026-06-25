package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.server.ServerObserver;
import net.blockhost.anarchyclient.server.ServerProfileStore;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ServerFingerprintHudModule extends HudElementModule {

    private final BooleanSetting store = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("store")
            .name("Store")
            .defaultValue(true)
            .build()));
    private String lastStored = "";

    public ServerFingerprintHudModule() {
        super("server_fingerprint_hud", "Server Fingerprint HUD", "Top Left");
    }

    @Override
    public void tick(final Minecraft client) {
        ServerObserver.Fingerprint fingerprint = ServerObserver.fingerprint();
        String key = fingerprint.rootDomain() + "|" + fingerprint.antiCheat() + "|" + fingerprint.labels()
                + "|" + fingerprint.plugins() + "|" + fingerprint.payloadChannels() + "|" + fingerprint.safetyScore();
        if (this.store.value() && !fingerprint.rootDomain().isBlank() && !key.equals(this.lastStored)) {
            ServerProfileStore.recordFingerprint(fingerprint);
            this.lastStored = key;
        }
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        ServerObserver.Snapshot snapshot = ServerObserver.snapshot();
        List<String> lines = new ArrayList<>();
        lines.add("Server " + fallback(snapshot.rootDomain(), "unknown"));
        lines.add("Type " + snapshot.serverType());
        snapshot.antiCheat().ifPresent(value -> lines.add("AC " + value));
        snapshot.tps().ifPresent(value -> lines.add(String.format("TPS %.1f", value)));
        lines.add("Safety " + snapshot.safetyScore() + "/100");
        if (!snapshot.environmentLabels().isEmpty()) {
            lines.add("Labels " + String.join(", ", snapshot.environmentLabels().stream().sorted().toList()));
        }
        if (!snapshot.plugins().isEmpty()) {
            lines.add("Plugins " + snapshot.plugins().stream().sorted().limit(5).toList());
        }
        if (!snapshot.payloadChannels().isEmpty()) {
            lines.add("Payloads " + snapshot.payloadChannels().stream()
                    .map(Object::toString)
                    .sorted(Comparator.naturalOrder())
                    .limit(4)
                    .toList());
        }
        return lines;
    }

    private static String fallback(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
