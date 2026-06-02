package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Random;

public final class ChatSpammerModule extends Module {

    private final StringSetting messages = this.setting(StringSetting.from(StringSetting.builder()
            .id("messages")
            .name("Messages")
            .defaultValue("AnarchyClient on top|gg|6b6t moment")
            .build()));
    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Sequential")
            .addAllOptions(List.of("Sequential", "Random"))
            .build()));
    private final NumberSetting intervalSeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("interval_seconds")
            .name("Interval")
            .defaultValue(30.0)
            .min(10.0)
            .max(300.0)
            .step(5.0)
            .build()));
    private final Random random = new Random();
    private int cooldownTicks;
    private int messageIndex;

    public ChatSpammerModule() {
        super("chat_spammer", "Chat Spammer", ModuleCategory.FUN);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.getConnection() == null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        List<String> parsedMessages = parseMessages(this.messages.value());
        if (parsedMessages.isEmpty()) {
            this.schedule();
            return;
        }
        client.getConnection().sendChat(this.nextMessage(parsedMessages));
        this.schedule();
    }

    @Override
    protected void onEnable() {
        this.schedule();
    }

    static List<String> parseMessages(final String value) {
        return java.util.Arrays.stream(value.split("\\|"))
                .map(String::trim)
                .filter(message -> !message.isEmpty())
                .toList();
    }

    private String nextMessage(final List<String> parsedMessages) {
        if ("Random".equals(this.mode.value())) {
            return parsedMessages.get(this.random.nextInt(parsedMessages.size()));
        }
        String message = parsedMessages.get(this.messageIndex % parsedMessages.size());
        this.messageIndex++;
        return message;
    }

    private void schedule() {
        this.cooldownTicks = Math.max(1, (int) Math.round(this.intervalSeconds.value() * 20));
    }
}
