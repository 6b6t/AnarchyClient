package net.blockhost.anarchyclient.event;

import net.minecraft.client.Minecraft;

public final class SendChatEvent implements AnarchyClientEvent {

    private final Minecraft client;
    private final boolean command;
    private String message;

    public SendChatEvent(final Minecraft client, final String message, final boolean command) {
        this.client = client;
        this.message = message;
        this.command = command;
    }

    public Minecraft client() {
        return this.client;
    }

    public String message() {
        return this.message;
    }

    public void message(final String message) {
        this.message = message;
    }

    public boolean command() {
        return this.command;
    }
}
