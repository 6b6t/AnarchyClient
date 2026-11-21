package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HudTextModulesTest {

    @Test
    void entityListSortsByCountThenNameAndSummarizesOverflow() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("Zombie", 2);
        counts.put("Arrow", 5);
        counts.put("Cow", 2);
        counts.put("Player", 1);

        assertEquals(List.of("Arrow x5", "Cow x2", "Zombie x2", "+1 more"), EntityListHudModule.format(counts, 3));
    }

    @Test
    void entityListShowsAllEntriesThatFit() {
        assertEquals(List.of("Cow x1"), EntityListHudModule.format(Map.of("Cow", 1), 5));
        assertEquals(List.of(), EntityListHudModule.format(Map.of(), 5));
    }

    @Test
    void welcomerGreetsByTimeOfDay() {
        assertEquals("Good night", WelcomerHudModule.timeGreeting(LocalTime.of(4, 59)));
        assertEquals("Good morning", WelcomerHudModule.timeGreeting(LocalTime.of(5, 0)));
        assertEquals("Good afternoon", WelcomerHudModule.timeGreeting(LocalTime.of(12, 0)));
        assertEquals("Good evening", WelcomerHudModule.timeGreeting(LocalTime.of(18, 30)));
        assertEquals("Good night", WelcomerHudModule.timeGreeting(LocalTime.of(22, 0)));
    }
}
