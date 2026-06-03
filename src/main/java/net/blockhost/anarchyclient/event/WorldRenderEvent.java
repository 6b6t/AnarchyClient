package net.blockhost.anarchyclient.event;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;

public record WorldRenderEvent(LevelRenderContext context) implements AnarchyClientEvent {
}
