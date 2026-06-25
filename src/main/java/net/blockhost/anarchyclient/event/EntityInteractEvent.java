package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.EntityHitResult;

public final class EntityInteractEvent extends CancellableAnarchyClientEvent {

    private final Minecraft client;
    private final Player player;
    private final Entity entity;
    private final EntityHitResult hitResult;
    private final InteractionHand hand;

    public EntityInteractEvent(final Minecraft client, final Player player, final Entity entity,
                               final EntityHitResult hitResult, final InteractionHand hand) {
        this.client = client;
        this.player = player;
        this.entity = entity;
        this.hitResult = hitResult;
        this.hand = hand;
    }

    public Minecraft client() {
        return this.client;
    }

    public Player player() {
        return this.player;
    }

    public Entity entity() {
        return this.entity;
    }

    public EntityHitResult hitResult() {
        return this.hitResult;
    }

    public InteractionHand hand() {
        return this.hand;
    }
}
