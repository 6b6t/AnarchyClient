package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class PreventEdgeFallEvent extends CancellableAnarchyClientEvent {

    private final Minecraft client;
    private final Player player;

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
        this.cancel();
    }
}
