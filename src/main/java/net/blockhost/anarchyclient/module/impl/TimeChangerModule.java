package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.ClientClockManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.util.Mth;
import net.minecraft.world.clock.ClockNetworkState;
import net.minecraft.world.clock.WorldClock;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class TimeChangerModule extends Module {

    private final NumberSetting time = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("time")
            .name("Time")
            .defaultValue(6000.0)
            .min(0.0)
            .max(24000.0)
            .step(500.0)
            .build()));
    /**
     * Clock updates from server time packets. Packets arrive on the network thread, so the client tick applies them.
     */
    private final Map<Holder<WorldClock>, ClockNetworkState> pendingServerUpdates = new ConcurrentHashMap<>();
    private @Nullable Holder<WorldClock> clock;
    private @Nullable ClockNetworkState serverState;
    private long serverStateGameTime;

    public TimeChangerModule() {
        super("time_changer", "Time Changer", ModuleCategory.RENDER,
                "Freezes the visible time of day at a chosen value on your client and restores the server time when disabled.");
    }

    @Override
    public void tick(final Minecraft client) {
        ClientLevel level = client.level;
        if (level == null) {
            this.forget();
            return;
        }
        Optional<Holder<WorldClock>> defaultClock = level.dimensionType().defaultClock();
        if (defaultClock.isEmpty()) {
            this.restore(level);
            return;
        }
        Holder<WorldClock> current = defaultClock.get();
        if (!current.equals(this.clock)) {
            this.restore(level);
            this.capture(level, current);
        }
        ClockNetworkState serverUpdate = this.pendingServerUpdates.get(current);
        this.pendingServerUpdates.clear();
        if (serverUpdate != null) {
            this.serverState = serverUpdate;
            this.serverStateGameTime = level.getGameTime();
        }
        this.apply(level, current, new ClockNetworkState(this.time.value().longValue(), 0.0F, 0.0F));
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        // Vanilla still applies the packet: it carries the game time and other clocks too. The tick, which runs
        // after packets are handled and before the frame renders, puts the frozen time back on the default clock.
        if (packet instanceof ClientboundSetTimePacket timePacket) {
            this.pendingServerUpdates.putAll(timePacket.clockUpdates());
        }
        return false;
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.forget();
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.level != null) {
            this.restore(client.level);
        } else {
            this.forget();
        }
    }

    static ClockNetworkState advance(final ClockNetworkState state, final long elapsedGameTicks) {
        double partialTicks = state.partialTick() + (double) elapsedGameTicks * state.rate();
        long fullTicks = Mth.lfloor(partialTicks);
        return new ClockNetworkState(state.totalTicks() + fullTicks, (float) (partialTicks - fullTicks), state.rate());
    }

    private void capture(final ClientLevel level, final Holder<WorldClock> clock) {
        ClientClockManager.ClientClockInstance instance = level.clockManager().getInstance(clock);
        this.clock = clock;
        this.serverState = new ClockNetworkState(instance.totalTicks(), instance.partialTick(), instance.rate());
        this.serverStateGameTime = level.getGameTime();
    }

    private void restore(final ClientLevel level) {
        if (this.clock != null && this.serverState != null) {
            this.apply(level, this.clock, advance(this.serverState, level.getGameTime() - this.serverStateGameTime));
        }
        this.forget();
    }

    private void apply(final ClientLevel level, final Holder<WorldClock> clock, final ClockNetworkState state) {
        level.clockManager().handleUpdates(level.getGameTime(), Map.of(clock, state));
        level.environmentAttributes().invalidateTickCache();
    }

    private void forget() {
        this.clock = null;
        this.serverState = null;
        this.serverStateGameTime = 0L;
        this.pendingServerUpdates.clear();
    }
}
