package net.blockhost.anarchyclient.module.impl;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterChatModuleTest {

    private final BetterChatModule module = new BetterChatModule();

    @AfterEach
    void disable() {
        this.module.enabled(false);
    }

    @Test
    void countKeepsTheOriginalFormattingAndClickEvents() {
        ClickEvent click = new ClickEvent.RunCommand("/spawn");
        Component original = Component.literal("Click ")
                .withStyle(style -> style.withClickEvent(click))
                .append(Component.literal("here").withStyle(ChatFormatting.RED));

        Component counted = BetterChatModule.withCount(original, 3);

        assertEquals("Click here x3", counted.getString());
        assertEquals(click, counted.getStyle().getClickEvent());
        assertEquals(original.getSiblings().getFirst(), counted.getSiblings().getFirst());
    }

    @Test
    void repeatedMessageReplacesThePreviousLine() {
        this.module.enabled(true);
        Component message = Component.literal("hello");

        this.module.chatMessage(null, message);
        assertFalse(BetterChatModule.takeReplacesPrevious());

        this.module.chatMessage(null, message);
        assertTrue(BetterChatModule.takeReplacesPrevious());
        assertFalse(BetterChatModule.takeReplacesPrevious());

        this.module.chatMessage(null, message);
        assertTrue(BetterChatModule.takeReplacesPrevious());
    }

    @Test
    void differentMessageDoesNotReplaceAnything() {
        this.module.enabled(true);

        this.module.chatMessage(null, Component.literal("hello"));
        this.module.chatMessage(null, Component.literal("world"));

        assertFalse(BetterChatModule.takeReplacesPrevious());
    }
}
