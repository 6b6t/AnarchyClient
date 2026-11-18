package net.blockhost.anarchyclient.command;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandPrefixTest {

    @Test
    void allowsOnlyASingleSymbolOtherThanSlash() {
        assertTrue(CommandPrefix.isAllowed("."));
        assertTrue(CommandPrefix.isAllowed("!"));
        // A letter or digit would swallow every chat line that starts with it.
        assertFalse(CommandPrefix.isAllowed("a"));
        assertFalse(CommandPrefix.isAllowed("7"));
        assertFalse(CommandPrefix.isAllowed("/"));
        assertFalse(CommandPrefix.isAllowed(" "));
        assertFalse(CommandPrefix.isAllowed(".."));
    }
}
