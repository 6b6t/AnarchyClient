package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntityOwnerModuleTest {

    private static final UUID OWNER = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    @Test
    void labelsKnownOwnersByName() {
        assertEquals("Owner: Notch", EntityOwnerModule.ownerLabel("Notch", OWNER));
    }

    @Test
    void fallsBackToTheShortUuidWhenTheOwnerIsNotInTheTabList() {
        assertEquals("Owner: 069a79f4", EntityOwnerModule.ownerLabel(null, OWNER));
        assertEquals("Owner: 069a79f4", EntityOwnerModule.ownerLabel(" ", OWNER));
    }
}
