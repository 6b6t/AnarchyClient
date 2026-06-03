package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record HudRenderEvent(Minecraft client, GuiGraphicsExtractor graphics) implements AnarchyClientEvent {
}
