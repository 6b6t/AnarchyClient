package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;

public record SoundPacketEvent(Minecraft client, ClientboundSoundPacket packet) implements AnarchyClientEvent {
}
