package net.blockhost.anarchyclient.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimedRequestQueueTest {

    @Test
    void returnsHighestPriorityRequest() {
        TimedRequestQueue<String> queue = new TimedRequestQueue<>();

        queue.request("normal", 0, 10, "normal");
        queue.request("important", 50, 10, "important");

        assertEquals("important", queue.activeValue().orElseThrow());
    }

    @Test
    void replacesRequestsFromSameOwner() {
        TimedRequestQueue<String> queue = new TimedRequestQueue<>();

        queue.request("owner", 0, 10, "old");
        queue.request("owner", 0, 10, "new");

        assertEquals("new", queue.activeValue().orElseThrow());
    }

    @Test
    void expiresRequestsAfterLifetimeTicks() {
        TimedRequestQueue<String> queue = new TimedRequestQueue<>();

        queue.request("short", 10, 1, "short");
        assertEquals("short", queue.activeValue().orElseThrow());

        queue.tick();

        assertTrue(queue.activeValue().isEmpty());
    }

    @Test
    void fallsBackToLowerPriorityWhenTopRequestExpires() {
        TimedRequestQueue<String> queue = new TimedRequestQueue<>();

        queue.request("long", 0, 10, "long");
        queue.request("short", 10, 1, "short");
        queue.tick();

        assertEquals("long", queue.activeValue().orElseThrow());
    }

    @Test
    void rejectsBlankOwners() {
        TimedRequestQueue<String> queue = new TimedRequestQueue<>();

        assertThrows(IllegalArgumentException.class, () -> queue.request(" ", 0, 1, "value"));
    }
}
