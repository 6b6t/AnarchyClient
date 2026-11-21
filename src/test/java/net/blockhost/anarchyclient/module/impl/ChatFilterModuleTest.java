package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ChatFilterModuleTest {

    @Test
    void plainFiltersMatchAnywhereIgnoringCase() {
        List<Pattern> patterns = ChatFilterModule.compile(List.of("Discord.GG/"), false);

        assertTrue(ChatFilterModule.matches(patterns, "<spam> join DISCORD.gg/abc now"));
        assertFalse(ChatFilterModule.matches(patterns, "<friend> meet at discord later"));
    }

    @Test
    void plainFiltersTreatRegexCharactersLiterally() {
        List<Pattern> patterns = ChatFilterModule.compile(List.of("a.b"), false);

        assertTrue(ChatFilterModule.matches(patterns, "see a.b"));
        assertFalse(ChatFilterModule.matches(patterns, "see axb"));
    }

    @Test
    void regexFiltersUseExpressions() {
        List<Pattern> patterns = ChatFilterModule.compile(List.of("^<\\w+> buy"), true);

        assertTrue(ChatFilterModule.matches(patterns, "<Seller> BUY cheap kits"));
        assertFalse(ChatFilterModule.matches(patterns, "<Player> want to buy?"));
    }

    @Test
    void skipsInvalidAndBlankEntries() {
        List<Pattern> patterns = ChatFilterModule.compile(List.of("(", " ", "spam"), true);

        assertEquals(1, patterns.size());
        assertTrue(ChatFilterModule.matches(patterns, "spam"));
    }
}
