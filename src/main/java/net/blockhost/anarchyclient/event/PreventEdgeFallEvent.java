package net.blockhost.anarchyclient.event;

import net.lenni0451.lambdaevents.types.ICancellableEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class PreventEdgeFallEvent implements AnarchyClientEvent, ICancellableEvent {

    private final Minecraft client;
    private final Player player;
    private boolean cancelled;

    public PreventEdgeFallEvent(final Minecraft client, final Player player) {
        this.client = client;
        this.player = player;
    }

    public Minecraft client() {
        return this.client;
    }

    public Player player() {
        return this.player;
    }

    public void prevent() {
        this.cancelled = true;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }
}
