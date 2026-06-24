package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityTargetingTest {

    @Test
    void parsesFriendNamesFromCommonSeparators() {
        assertEquals(Set.of("alex", "steve", "enderman"), EntityTargeting.parseNames(" Alex, Steve | enderman  "));
    }

    @Test
    void ignoresBlankFriendLists() {
        assertEquals(Set.of(), EntityTargeting.parseNames("  | ,  "));
    }

    @Test
    void detectsSuspiciousPlayerNames() {
        assertTrue(EntityTargeting.looksLikeBot("", 10));
        assertTrue(EntityTargeting.looksLikeBot("CIT-123", 10));
        assertTrue(EntityTargeting.looksLikeBot("NPCGuard", 10));
        assertTrue(EntityTargeting.looksLikeBot("123e4567-e89b-12d3-a456-426614174000", 10));
        assertTrue(EntityTargeting.looksLikeBot("Alex", 4));
    }

    @Test
    void acceptsNormalPlayerNamesAfterSpawnGrace() {
        assertFalse(EntityTargeting.looksLikeBot("Alex", 5));
        assertFalse(EntityTargeting.looksLikeBot("Steve_123", 20));
    }
}
