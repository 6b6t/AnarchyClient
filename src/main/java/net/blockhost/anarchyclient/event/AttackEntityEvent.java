package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class AttackEntityEvent extends CancellableAnarchyClientEvent {

    private final Minecraft client;
    private final Player player;
    private final Entity target;

    public AttackEntityEvent(final Minecraft client, final Player player, final Entity target) {
        this.client = client;
        this.player = player;
        this.target = target;
    }

    public Minecraft client() {
        return this.client;
    }

    public Player player() {
        return this.player;
    }

    public Entity target() {
        return this.target;
    }
}
