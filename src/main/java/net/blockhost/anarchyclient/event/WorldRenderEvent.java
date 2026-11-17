package net.blockhost.anarchyclient.event;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;

public record WorldRenderEvent(Minecraft client, LevelRenderContext context) implements AnarchyClientEvent {
}
