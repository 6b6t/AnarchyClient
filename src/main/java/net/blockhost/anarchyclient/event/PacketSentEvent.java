package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

public record PacketSentEvent(Minecraft client, Connection connection, Packet<?> packet) implements AnarchyClientEvent {
}
