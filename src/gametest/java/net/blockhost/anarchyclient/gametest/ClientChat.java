package net.blockhost.anarchyclient.gametest;

import net.blockhost.anarchyclient.gametest.mixin.ChatComponentAccessor;
import net.minecraft.client.Minecraft;

import java.util.List;

/** Reads the messages shown in the client's chat, which vanilla keeps private. */
final class ClientChat {

    private ClientChat() {
    }

    static List<String> messages(final Minecraft client) {
        return ((ChatComponentAccessor) client.gui.hud.getChat()).anarchyclientGametest$allMessages().stream()
                .map(message -> message.content().getString())
                .toList();
    }

    static boolean contains(final Minecraft client, final String text) {
        return messages(client).stream().anyMatch(message -> message.contains(text));
    }
}
