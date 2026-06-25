package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;

public final class MouseClickEvent extends CancellableAnarchyClientEvent {

    private final Minecraft client;
    private final MouseButtonInfo buttonInfo;
    private final int action;

    public MouseClickEvent(final Minecraft client, final MouseButtonInfo buttonInfo, final int action) {
        this.client = client;
        this.buttonInfo = buttonInfo;
        this.action = action;
    }

    public Minecraft client() {
        return this.client;
    }

    public MouseButtonInfo buttonInfo() {
        return this.buttonInfo;
    }

    public int action() {
        return this.action;
    }
}
