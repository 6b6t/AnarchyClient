package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;

public final class MouseScrollInputEvent extends CancellableAnarchyClientEvent {

    private final Minecraft client;
    private final double xOffset;
    private final double yOffset;

    public MouseScrollInputEvent(final Minecraft client, final double xOffset, final double yOffset) {
        this.client = client;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public Minecraft client() {
        return this.client;
    }

    public double xOffset() {
        return this.xOffset;
    }

    public double yOffset() {
        return this.yOffset;
    }
}
