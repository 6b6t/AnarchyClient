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
    private final BooleanSetting antiClear = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("anti_clear")
            .name("Anti Clear")
            .defaultValue(true)
            .build()));
    private final BooleanSetting compactDuplicates = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("compact_duplicates")
            .name("Compact")
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
    private static BetterChatModule active;
    private String lastMessage = "";
    private int duplicateCount;

    public BetterChatModule() {
        super("better_chat", "Better Chat", ModuleCategory.MISC);
    }

    @Override
    public Component chatMessage(final Minecraft client, final Component message) {
        if (message == null) {
            return message;
        }
        Component result = this.compactDuplicate(message);
        if (this.timestamps.value()) {
            result = withTimestamp(result, LocalTime.now());
        }
        return result;
    }

    @Override
    public String sendChatMessage(final Minecraft client, final String message, final boolean command) {
        if (!this.outgoingSuffix.value() || command || message == null || message.isBlank()) {
            return message;
        }
        return message + this.suffix.value();
    }

    @Override
    protected void onEnable() {
        active = this;
        this.lastMessage = "";
        this.duplicateCount = 0;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
        this.lastMessage = "";
        this.duplicateCount = 0;
    }

    public static boolean shouldPreventClearMessages() {
        return active != null && active.antiClear.value();
    }

    private Component compactDuplicate(final Component message) {
        if (!this.compactDuplicates.value()) {
            this.lastMessage = message.getString();
            this.duplicateCount = 1;
            return message;
        }
        String raw = message.getString();
        if (raw.equals(this.lastMessage)) {
            this.duplicateCount++;
            return Component.literal(raw + " x" + this.duplicateCount).withStyle(message.getStyle());
        }
        this.lastMessage = raw;
        this.duplicateCount = 1;
        return message;
    }

    static Component withTimestamp(final Component message, final LocalTime time) {
        return Component.literal("[" + TIME_FORMAT.format(time) + "] ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(message.copy());
    }
}
