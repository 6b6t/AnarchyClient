package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.network.PacketQueueManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class FakeLagModule extends Module {

    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(250.0)
            .min(50.0)
            .max(2000.0)
            .step(50.0)
            .build()));
    private final NumberSetting maxPackets = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_packets")
            .name("Packets")
            .defaultValue(64.0)
            .min(1.0)
            .max(512.0)
            .step(1.0)
            .build()));
    private final BooleanSetting movement = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("movement")
            .name("Move")
            .defaultValue(true)
            .build()));
    private final BooleanSetting dropOnDisable = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("drop_on_disable")
            .name("Drop")
            .defaultValue(false)
            .build()));

    public FakeLagModule() {
        super("fake_lag", "Fake Lag", ModuleCategory.MISC,
                "Holds back outgoing movement packets and releases them after a delay, making you appear to lag to other players.");
    }

    @Override
    public void tick(final Minecraft client) {
        PacketQueueManager.flushOutgoingOlderThan(this.id(), this.delay.value().longValue());
        this.debugValue("queued", PacketQueueManager.outgoingSize(this.id()));
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!this.movement.value() || client.hasSingleplayerServer() || !(packet instanceof ServerboundMovePlayerPacket)) {
            return false;
        }
        return PacketQueueManager.queueOutgoing(this.id(), connection, packet, this.maxPackets.value().intValue());
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        PacketQueueManager.dropOutgoing(this.id());
    }

    @Override
    protected void onDisable() {
        if (this.dropOnDisable.value()) {
            PacketQueueManager.dropOutgoing(this.id());
        } else {
            PacketQueueManager.flushOutgoing(this.id());
        }
        this.clearDebugValues();
    }
}
