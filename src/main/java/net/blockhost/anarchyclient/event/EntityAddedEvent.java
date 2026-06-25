package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public record EntityAddedEvent(Minecraft client, Entity entity) implements AnarchyClientEvent {
}
