package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

public final class PacketReceiveEvent extends CancellableAnarchyClientEvent {

    private final Minecraft client;
    private final Connection connection;
    private Packet<?> packet;

    public PacketReceiveEvent(final Minecraft client, final Connection connection, final Packet<?> packet) {
        this.client = client;
        this.connection = connection;
        this.packet = packet;
    }

    public Minecraft client() {
        return this.client;
    }

    public Connection connection() {
        return this.connection;
    }

    public Packet<?> packet() {
        return this.packet;
    }

    public void packet(final Packet<?> packet) {
        this.packet = packet;
    }
}
