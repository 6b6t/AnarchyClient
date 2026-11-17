package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoMissCooldownModuleTest {

    @Test
    void removesTheMissCooldownOnlyWhileEnabled() {
        NoMissCooldownModule module = new NoMissCooldownModule();
        assertFalse(NoMissCooldownModule.removesMissCooldown());

        module.enabled(true);
        assertTrue(NoMissCooldownModule.removesMissCooldown());

        module.enabled(false);
        assertFalse(NoMissCooldownModule.removesMissCooldown());
    }
}
