package net.blockhost.anarchyclient.module.impl;

import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanModulesTest {

    @Test
    void textFieldProtectMasksConfiguredFieldsOnly() {
        TextFieldProtectModule module = new TextFieldProtectModule();
        module.enabled(true);
        try {
            assertTrue(TextFieldProtectModule.shouldMask(Component.literal("Password"), "hunter2"));
            assertEquals("*******", TextFieldProtectModule.mask("hunter2"));
            assertFalse(TextFieldProtectModule.shouldMask(Component.literal("Server Name"), "example.org"));
        } finally {
            module.enabled(false);
        }
    }

    @Test
    void strongholdFinderIntersectsEyeThrowRays() {
        Optional<Vec3> estimate = StrongholdFinderModule.estimate(
                new Vec3(0.0, 64.0, 0.0),
                new Vec3(1.0, 0.0, 1.0),
                new Vec3(10.0, 70.0, 0.0),
                new Vec3(-1.0, 0.0, 1.0)
        );

        assertTrue(estimate.isPresent());
        assertEquals(5.0, estimate.orElseThrow().x, 1.0E-9);
        assertEquals(5.0, estimate.orElseThrow().z, 1.0E-9);
    }
}
