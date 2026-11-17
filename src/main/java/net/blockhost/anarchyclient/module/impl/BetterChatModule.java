package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

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
    private Component lastShown;
    private Component replacedMessage;

    public BetterChatModule() {
        super("better_chat", "Better Chat", ModuleCategory.MISC,
                "Adds timestamps to chat, merges repeated messages into one line with a count, blocks server chat clears, and can append a suffix to sent messages.");
    }

    @Override
    public Component chatMessage(final Minecraft client, final Component message) {
        if (message == null) {
            return message;
        }
        Component previous = this.lastShown;
        boolean duplicate = this.compactDuplicate(message);
        Component result = duplicate ? withCount(message, this.duplicateCount) : message;
        if (this.timestamps.value()) {
            result = withTimestamp(result, LocalTime.now());
        }
        this.replacedMessage = duplicate ? previous : null;
        this.lastShown = result;
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
        this.reset();
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
        this.reset();
    }

    public static boolean shouldPreventClearMessages() {
        return active != null && active.antiClear.value();
    }

    /**
     * Returns the chat line that the latest message repeats, so the chat can remove it. Each call clears it.
     */
    public static @Nullable Component takeReplacedMessage() {
        BetterChatModule module = active;
        if (module == null) {
            return null;
        }
        Component replaced = module.replacedMessage;
        module.replacedMessage = null;
        return replaced;
    }

    private void reset() {
        this.lastMessage = "";
        this.duplicateCount = 0;
        this.lastShown = null;
        this.replacedMessage = null;
    }

    private boolean compactDuplicate(final Component message) {
        String raw = message.getString();
        if (this.compactDuplicates.value() && raw.equals(this.lastMessage)) {
            this.duplicateCount++;
            return true;
        }
        this.lastMessage = raw;
        this.duplicateCount = 1;
        return false;
    }

    static Component withCount(final Component message, final int count) {
        return message.copy().append(Component.literal(" x" + count).withStyle(ChatFormatting.GRAY));
    }

    static Component withTimestamp(final Component message, final LocalTime time) {
        return Component.literal("[" + TIME_FORMAT.format(time) + "] ")
                .withStyle(ChatFormatting.DARK_GRAY)
                .append(message.copy());
    }
}
