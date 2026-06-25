package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class ItemStopUseEvent implements AnarchyClientEvent {

    private final Minecraft client;
    private final InteractionHand hand;
    private final ItemStack stack;
    private final int remainingTicks;

    public ItemStopUseEvent(final Minecraft client, final InteractionHand hand, final ItemStack stack,
                            final int remainingTicks) {
        this.client = client;
        this.hand = hand;
        this.stack = stack;
        this.remainingTicks = remainingTicks;
    }

    public Minecraft client() {
        return this.client;
    }

    public InteractionHand hand() {
        return this.hand;
    }

    public ItemStack stack() {
        return this.stack;
    }

    public int remainingTicks() {
        return this.remainingTicks;
    }
}
