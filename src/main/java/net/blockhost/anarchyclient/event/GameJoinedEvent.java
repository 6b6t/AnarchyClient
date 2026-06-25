package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public record GameJoinedEvent(Minecraft client, ClientPacketListener listener) implements AnarchyClientEvent {
}
