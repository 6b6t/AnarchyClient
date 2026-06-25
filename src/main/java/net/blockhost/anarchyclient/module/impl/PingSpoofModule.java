package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.network.PacketQueueManager;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;

public final class PingSpoofModule extends Module {

    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(500.0)
            .min(0.0)
            .max(25_000.0)
            .step(50.0)
            .build()));
    private final NumberSetting maxPackets = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_packets")
            .name("Packets")
            .defaultValue(32.0)
            .min(1.0)
            .max(256.0)
            .step(1.0)
            .build()));

    public PingSpoofModule() {
        super("ping_spoof", "Ping Spoof", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.hasSingleplayerServer()) {
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("Ping Spoof is disabled in singleplayer."));
            }
            this.enabled(false);
            return;
        }
        PacketQueueManager.flushOlderThan(this.delay.value().longValue());
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.hasSingleplayerServer() || !(packet instanceof ClientboundKeepAlivePacket || packet instanceof ClientboundPingPacket)) {
            return false;
        }
        return PacketQueueManager.queueIncoming(connection, packet, this.maxPackets.value().intValue());
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        PacketQueueManager.dropAll();
    }

    @Override
    protected void onDisable() {
        PacketQueueManager.flushAll();
    }
}
