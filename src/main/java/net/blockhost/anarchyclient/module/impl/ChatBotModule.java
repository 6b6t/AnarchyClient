package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.util.PlaceholderFormatter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

public final class ChatBotModule extends Module {

    /** Replies waiting at once. Anyone spamming a trigger would otherwise make the bot spam for them. */
    static final int MAX_QUEUED_REPLIES = 3;

    private final StringSetting prefix = this.setting(StringSetting.from(StringSetting.builder()
            .id("prefix")
            .name("Prefix")
            .defaultValue("!")
            .build()));
    private final StringSetting replies = this.setting(StringSetting.from(StringSetting.builder()
            .id("replies")
            .name("Replies")
            .defaultValue("ping=Pong, {player.name}.")
            .description("One key=value reply per line.")
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(10.0)
            .min(0.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private final Queue<QueuedReply> queue = new ArrayDeque<>();

    public ChatBotModule() {
        super("chat_bot", "Chat Bot", ModuleCategory.MISC,
                "Replies in chat when a message contains the prefix and a configured keyword, filling in placeholders like player name and coordinates.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.getConnection() == null || this.queue.isEmpty()) {
            return;
        }
        QueuedReply reply = this.queue.peek();
        if (reply == null || --reply.ticks > 0) {
            return;
        }
        this.queue.remove();
        ChatActions.send(client, reply.message);
    }

    @Override
    public Component chatMessage(final Minecraft client, final Component message) {
        String command = command(message.getString(), this.prefix.value());
        if (command == null || client.player == null) {
            return message;
        }
        String reply = parseReplies(this.replies.value()).get(command.toLowerCase(Locale.ROOT));
        if (reply != null) {
            String formatted = PlaceholderFormatter.format(reply, placeholders(client));
            if (canQueue(this.queue.stream().map(queued -> queued.message).toList(), formatted)) {
                this.queue.add(new QueuedReply(Math.max(0, this.delay.value().intValue()), formatted));
            }
        }
        return message;
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.queue.clear();
    }

    /** A reply is queued only when the queue has room and the same reply is not already waiting. */
    static boolean canQueue(final List<String> queued, final String reply) {
        return queued.size() < MAX_QUEUED_REPLIES && !queued.contains(reply);
    }

    static Map<String, String> parseReplies(final String value) {
        Map<String, String> result = new LinkedHashMap<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String line : value.split("\\R|\\\\n")) {
            int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = line.substring(0, equals).trim().toLowerCase(Locale.ROOT);
            String reply = line.substring(equals + 1).trim();
            if (!key.isBlank() && !reply.isBlank()) {
                result.put(key, reply);
            }
        }
        return result;
    }

    static String command(final String rawMessage, final String prefix) {
        if (rawMessage == null || prefix == null || prefix.isBlank()) {
            return null;
        }
        int index = rawMessage.indexOf(prefix);
        if (index < 0) {
            return null;
        }
        String command = rawMessage.substring(index + prefix.length()).trim();
        int space = command.indexOf(' ');
        return space < 0 ? command : command.substring(0, space);
    }

    private static Map<String, String> placeholders(final Minecraft client) {
        Map<String, String> placeholders = new LinkedHashMap<>();
        if (client.player != null) {
            placeholders.put("player.name", client.player.getScoreboardName());
            placeholders.put("player.x", Integer.toString(client.player.blockPosition().getX()));
            placeholders.put("player.y", Integer.toString(client.player.blockPosition().getY()));
            placeholders.put("player.z", Integer.toString(client.player.blockPosition().getZ()));
            placeholders.put("player.pos", client.player.blockPosition().getX() + ", "
                    + client.player.blockPosition().getY() + ", " + client.player.blockPosition().getZ());
        }
        if (client.level != null) {
            placeholders.put("dimension", client.level.dimension().identifier().toString());
        }
        if (client.getCurrentServer() != null) {
            placeholders.put("server.address", client.getCurrentServer().ip);
        }
        placeholders.put("server.brand", client.getConnection() == null || client.getConnection().serverBrand() == null
                ? "unknown" : client.getConnection().serverBrand());
        return placeholders;
    }

    private static final class QueuedReply {
        private int ticks;
        private final String message;

        private QueuedReply(final int ticks, final String message) {
            this.ticks = ticks;
            this.message = message;
        }
    }
}
