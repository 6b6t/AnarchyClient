package net.blockhost.anarchyclient.target;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TargetClassifierTest {

    @Test
    void detectsSuspiciousPlayerNames() {
        assertTrue(TargetClassifier.looksLikeBot("", 10));
        assertTrue(TargetClassifier.looksLikeBot("CIT-123", 10));
        assertTrue(TargetClassifier.looksLikeBot("NPCGuard", 10));
        assertTrue(TargetClassifier.looksLikeBot("123e4567-e89b-12d3-a456-426614174000", 10));
        assertTrue(TargetClassifier.looksLikeBot("Alex", 4));
    }

    @Test
    void acceptsNormalPlayerNamesAfterSpawnGrace() {
        assertFalse(TargetClassifier.looksLikeBot("Alex", 5));
        assertFalse(TargetClassifier.looksLikeBot("Steve_123", 20));
    }
}
