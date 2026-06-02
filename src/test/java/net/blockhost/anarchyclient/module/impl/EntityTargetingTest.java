package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityTargetingTest {

    @Test
    void parsesFriendNamesFromCommonSeparators() {
        assertEquals(Set.of("alex", "steve", "enderman"), EntityTargeting.parseNames(" Alex, Steve | enderman  "));
    }

    @Test
    void ignoresBlankFriendLists() {
        assertEquals(Set.of(), EntityTargeting.parseNames("  | ,  "));
    }
}
