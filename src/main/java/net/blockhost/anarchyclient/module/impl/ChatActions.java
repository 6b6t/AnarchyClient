package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;

final class ChatActions {

    private ChatActions() {
    }

    static void send(final Minecraft client, final String message) {
        if (client.getConnection() == null || message == null || message.isBlank()) {
            return;
        }
        String trimmed = message.trim();
        if (trimmed.startsWith("/")) {
            client.getConnection().sendCommand(trimmed.substring(1));
        } else {
            client.getConnection().sendChat(trimmed);
        }
    }
}
