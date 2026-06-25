package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class CameraTransformEvent implements AnarchyClientEvent {

    private final Minecraft client;
    private Vec3 position;
    private float yaw;
    private float pitch;

    public CameraTransformEvent(final Minecraft client, final Vec3 position, final float yaw, final float pitch) {
        this.client = client;
        this.position = position;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public Minecraft client() {
        return this.client;
    }

    public Vec3 position() {
        return this.position;
    }

    public void position(final Vec3 position) {
        this.position = position;
    }

    public float yaw() {
        return this.yaw;
    }

    public void yaw(final float yaw) {
        this.yaw = yaw;
    }

    public float pitch() {
        return this.pitch;
    }

    public void pitch(final float pitch) {
        this.pitch = pitch;
    }
}
