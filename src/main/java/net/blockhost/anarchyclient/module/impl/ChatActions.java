package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.command.ClientCommands;
import net.blockhost.anarchyclient.command.CommandPrefix;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.util.StringUtil;

public final class ChatActions {

    private ChatActions() {}

    /**
     * Sends chat text, or a server command when it starts with {@code /}. Text that starts with the client command
     * prefix is sent as chat: generated messages (spammer lines, replies, Swarm actions) must not run client commands.
     */
    public static void send(final Minecraft client, final String message) {
        send(client, message, false);
    }

    /**
     * Like {@link #send}, but a line that starts with the client command prefix runs that client command, the way
     * typing it into chat would. Only for actions the player configured themselves (macros, click actions).
     */
    public static void run(final Minecraft client, final String message) {
        send(client, message, true);
    }

    private static void send(final Minecraft client, final String message, final boolean clientCommands) {
        ClientPacketListener connection = client.getConnection();
        if (connection == null || message == null) {
            return;
        }
        String normalized = normalize(message);
        if (normalized.isBlank() || "/".equals(normalized)) {
            return;
        }
        if (clientCommands && normalized.startsWith(CommandPrefix.get())) {
            ClientCommands.execute(connection, normalized.substring(1).trim());
        } else if (normalized.startsWith("/")) {
            connection.sendCommand(normalized.substring(1));
        } else {
            connection.sendChat(normalized);
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
