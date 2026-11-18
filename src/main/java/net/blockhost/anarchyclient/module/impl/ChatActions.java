package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.util.StringUtil;

public final class ChatActions {

    private ChatActions() {
    }

    /** Sends chat text, or a command when it starts with {@code /}, the way the chat screen would. */
    public static void send(final Minecraft client, final String message) {
        if (client.getConnection() == null || message == null) {
            return;
        }
        String normalized = normalize(message);
        if (normalized.isBlank() || normalized.equals("/")) {
            return;
        }
        if (normalized.startsWith("/")) {
            client.getConnection().sendCommand(normalized.substring(1));
        } else {
            client.getConnection().sendChat(normalized);
        }
    }

    /**
     * Applies the chat screen's limits. The chat packet cannot encode more than 256 characters (the client
     * disconnects with an encoder error), and the server kicks for section signs or control characters.
     */
    static String normalize(final String message) {
        return StringUtil.trimChatMessage(StringUtil.filterText(message.trim()).trim());
    }
}
