package net.blockhost.anarchyclient.ui;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HudLayoutTest {

    @Test
    void aMalformedEntryOnlyDropsThatEntry() {
        var positions = HudLayout.readPositions(JsonParser.parseString("""
                {"a": {"x": 1, "y": 2}, "b": {"x": "abc", "y": 3}, "c": {"x": 5, "y": 6}}
                """).getAsJsonObject());

        assertEquals(List.of("a", "c"), List.copyOf(positions.keySet()));
        assertArrayEquals(new int[]{5, 6}, positions.get("c"));
    }
}
