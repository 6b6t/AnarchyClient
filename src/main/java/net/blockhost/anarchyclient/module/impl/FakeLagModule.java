package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.network.PacketQueueManager;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
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
        super("fake_lag", "Fake Lag", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        PacketQueueManager.flushOlderThan(this.delay.value().longValue());
        if (client.player != null && (PacketQueueManager.incomingSize() > 0 || PacketQueueManager.outgoingSize() > 0)
                && client.player.tickCount % 40 == 0) {
            client.player.sendSystemMessage(Component.literal("Fake Lag queue: "
                    + PacketQueueManager.incomingSize() + " in / " + PacketQueueManager.outgoingSize() + " out."));
        }
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!this.movement.value() || client.hasSingleplayerServer() || !(packet instanceof ServerboundMovePlayerPacket)) {
            return false;
        }
        return PacketQueueManager.queueOutgoing(connection, packet, this.maxPackets.value().intValue());
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        PacketQueueManager.dropAll();
    }

    @Override
    protected void onDisable() {
        if (this.dropOnDisable.value()) {
            PacketQueueManager.dropAll();
        } else {
            PacketQueueManager.flushAll();
        }
    }
}
