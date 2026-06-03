package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;

public record ClientTickEvent(Minecraft client) implements AnarchyClientEvent {
}
