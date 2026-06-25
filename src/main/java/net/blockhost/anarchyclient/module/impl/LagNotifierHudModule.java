package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

import java.util.List;

public final class LagNotifierHudModule extends HudElementModule {

    private final NumberSetting thresholdSeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("threshold_seconds")
            .name("Threshold")
            .defaultValue(2.0)
            .min(0.5)
            .max(10.0)
            .step(0.5)
            .build()));
    private final BooleanSetting onlyWhenLagging = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("only_when_lagging")
            .name("Only Lagging")
            .defaultValue(true)
            .build()));
    private long lastPacketNanos = System.nanoTime();

    public LagNotifierHudModule() {
        super("lag_notifier_hud", "Lag Notifier", "Top Left");
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        this.lastPacketNanos = System.nanoTime();
        return false;
    }

    @Override
    protected int color() {
        return 0xFFE6C76E;
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        double seconds = this.secondsSincePacket();
        if (this.onlyWhenLagging.value() && seconds < this.thresholdSeconds.value()) {
            return List.of();
        }
        return List.of("Server " + String.format("%.1fs", seconds));
    }

    double secondsSincePacket() {
        return (System.nanoTime() - this.lastPacketNanos) / 1_000_000_000.0;
    }
}
