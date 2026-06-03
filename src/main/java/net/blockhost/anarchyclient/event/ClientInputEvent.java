package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;

public record ClientInputEvent(Minecraft client, ClientInput input) implements AnarchyClientEvent {
}
