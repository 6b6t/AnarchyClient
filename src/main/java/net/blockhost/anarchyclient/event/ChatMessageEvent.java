package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ChatMessageEvent implements AnarchyClientEvent {

    private final Minecraft client;
    private Component message;

    public ChatMessageEvent(final Minecraft client, final Component message) {
        this.client = client;
        this.message = message;
    }

    public Minecraft client() {
        return this.client;
    }

    public Component message() {
        return this.message;
    }

    public void message(final Component message) {
        this.message = message;
    }
}
