package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

public final class BlockInteractEvent extends CancellableAnarchyClientEvent {

    private final Minecraft client;
    private final InteractionHand hand;
    private final BlockHitResult hitResult;

    public BlockInteractEvent(final Minecraft client, final InteractionHand hand, final BlockHitResult hitResult) {
        this.client = client;
        this.hand = hand;
        this.hitResult = hitResult;
    }

    public Minecraft client() {
        return this.client;
    }

    public InteractionHand hand() {
        return this.hand;
    }

    public BlockHitResult hitResult() {
        return this.hitResult;
    }
}
