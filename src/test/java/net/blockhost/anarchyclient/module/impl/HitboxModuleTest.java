package net.blockhost.anarchyclient.module.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HitboxModuleTest {

    @Test
    void growsClientLivingEntities() {
        assertEquals(0.4F, HitboxModule.pickRadiusBonus(true, 0.4), 1.0E-6F);
    }

    @Test
    void leavesOtherEntitiesAlone() {
        assertEquals(0.0F, HitboxModule.pickRadiusBonus(false, 0.4));
    }

    @Test
    void addsNothingWhileInactive() {
        assertEquals(0.0F, HitboxModule.pickRadiusBonus(true, 0.0));
    }
}
