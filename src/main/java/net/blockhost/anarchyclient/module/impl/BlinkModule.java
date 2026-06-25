package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.event.PacketEventSilencer;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.ArrayDeque;
import java.util.Queue;

public final class BlinkModule extends Module {

    private final NumberSetting maxPackets = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_packets")
            .name("Packets")
            .defaultValue(120.0)
            .min(1.0)
            .max(512.0)
            .step(1.0)
            .build()));
    private final BooleanSetting dropOverflow = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("drop_overflow")
            .name("Drop Overflow")
            .defaultValue(false)
            .build()));
    private final Queue<QueuedPacket> packets = new ArrayDeque<>();

    public BlinkModule() {
        super("blink", "Blink", ModuleCategory.MOVEMENT);
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.hasSingleplayerServer() || !(packet instanceof ServerboundMovePlayerPacket)) {
            return false;
        }
        if (this.packets.size() >= this.maxPackets.value().intValue()) {
            if (!this.dropOverflow.value()) {
                this.flush();
            }
            return this.dropOverflow.value();
        }
        this.packets.add(new QueuedPacket(connection, packet));
        return true;
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.packets.clear();
    }

    @Override
    protected void onDisable() {
        this.flush();
    }

    private void flush() {
        while (!this.packets.isEmpty()) {
            QueuedPacket queued = this.packets.remove();
            PacketEventSilencer.runSilently(() -> queued.connection().send(queued.packet()));
        }
    }

    private record QueuedPacket(Connection connection, Packet<?> packet) {
    }
}
