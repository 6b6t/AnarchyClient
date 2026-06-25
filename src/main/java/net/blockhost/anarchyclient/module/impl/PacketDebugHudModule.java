package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.DebugValueRegistry;
import net.blockhost.anarchyclient.network.PacketQueueManager;
import net.blockhost.anarchyclient.timer.TimerManager;
import net.blockhost.anarchyclient.timer.TimerBalanceService;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PacketDebugHudModule extends HudElementModule {

    public PacketDebugHudModule() {
        super("packet_debug_hud", "Packet Debug HUD", "Bottom Right");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        PacketQueueManager.Snapshot queue = PacketQueueManager.snapshot();
        List<String> lines = new ArrayList<>();
        lines.add("Queue " + queue.incomingSize() + " in / " + queue.outgoingSize() + " out");
        if (queue.oldestIncomingAgeMillis() > 0 || queue.oldestOutgoingAgeMillis() > 0) {
            lines.add("Age " + queue.oldestIncomingAgeMillis() + " / " + queue.oldestOutgoingAgeMillis() + " ms");
        }
        TimerManager.activeMultiplier().ifPresent(value -> lines.add(String.format("Timer %.2fx", value)));
        double tickBaseBalance = TimerBalanceService.value("tick_base");
        double timerRangeBalance = TimerBalanceService.value("timer_range");
        if (tickBaseBalance > 0.0 || timerRangeBalance > 0.0) {
            lines.add(String.format("Balance %.1f / %.1f", tickBaseBalance, timerRangeBalance));
        }
        for (Map.Entry<String, Map<String, String>> owner : DebugValueRegistry.snapshot().entrySet()) {
            for (Map.Entry<String, String> value : owner.getValue().entrySet()) {
                lines.add(owner.getKey() + " " + value.getKey() + "=" + value.getValue());
                if (lines.size() >= 10) {
                    return lines;
                }
            }
        }
        return lines;
    }
}
