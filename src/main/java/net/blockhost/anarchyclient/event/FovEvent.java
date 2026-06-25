package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;

public final class FovEvent implements AnarchyClientEvent {

    private final Minecraft client;
    private float fov;

    public FovEvent(final Minecraft client, final float fov) {
        this.client = client;
        this.fov = fov;
    }

    public Minecraft client() {
        return this.client;
    }

    public float fov() {
        return this.fov;
    }

    public void fov(final float fov) {
        this.fov = fov;
    }
}
