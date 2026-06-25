package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public final class BetterChatModule extends Module {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final BooleanSetting timestamps = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("timestamps")
            .name("Timestamps")
            .defaultValue(true)
            .build()));
    private final BooleanSetting outgoingSuffix = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("outgoing_suffix")
            .name("Suffix")
            .defaultValue(false)
            .build()));
    private final StringSetting suffix = this.setting(StringSetting.from(StringSetting.builder()
            .id("suffix")
            .name("Text")
            .defaultValue(" | AnarchyClient")
            .build()));

    public BetterChatModule() {
        super("better_chat", "Better Chat", ModuleCategory.MISC);
    }

    @Override
    public Component chatMessage(final Minecraft client, final Component message) {
        if (!this.timestamps.value() || message == null) {
            return message;
        }
        return withTimestamp(message, LocalTime.now());
    }

    @Override
    public String sendChatMessage(final Minecraft client, final String message, final boolean command) {
        if (!this.outgoingSuffix.value() || command || message == null || message.isBlank()) {
            return message;
        }
        return message + this.suffix.value();
    }

    static Component withTimestamp(final Component message, final LocalTime time) {
        return Component.literal("[" + TIME_FORMAT.format(time) + "] ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(message.copy());
    }
}
