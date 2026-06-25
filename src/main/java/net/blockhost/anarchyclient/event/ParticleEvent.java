package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;

public final class ParticleEvent extends CancellableAnarchyClientEvent {

    private final Minecraft client;
    private final ParticleOptions particle;
    private final boolean alwaysShow;

    public ParticleEvent(final Minecraft client, final ParticleOptions particle, final boolean alwaysShow) {
        this.client = client;
        this.particle = particle;
        this.alwaysShow = alwaysShow;
    }

    public Minecraft client() {
        return this.client;
    }

    public ParticleOptions particle() {
        return this.particle;
    }

    public boolean alwaysShow() {
        return this.alwaysShow;
    }
}
