package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

public record GameLeftEvent(Minecraft client, ClientPacketListener listener) implements AnarchyClientEvent {
}
