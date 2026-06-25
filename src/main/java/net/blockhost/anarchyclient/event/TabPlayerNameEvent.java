package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class TabPlayerNameEvent implements AnarchyClientEvent {

    private final Minecraft client;
    private final PlayerInfo playerInfo;
    private Component name;

    public TabPlayerNameEvent(final Minecraft client, final PlayerInfo playerInfo, final Component name) {
        this.client = client;
        this.playerInfo = playerInfo;
        this.name = name;
    }

    public Minecraft client() {
        return this.client;
    }

    public PlayerInfo playerInfo() {
        return this.playerInfo;
    }

    public Component name() {
        return this.name;
    }

    public void name(final Component name) {
        this.name = name;
    }
}
