package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class StrongholdFinderModuleTest {

    @Test
    void waitsUntilTheEyeHasTravelledFarEnough() {
        assertTrue(StrongholdFinderModule.flightDirection(Vec3.ZERO, new Vec3(1.0, 3.0, 1.0))
                .isEmpty());
    }

    @Test
    void usesTheEyeHorizontalFlightDirection() {
        Optional<Vec3> direction =
                StrongholdFinderModule.flightDirection(new Vec3(10.0, 64.0, 10.0), new Vec3(13.0, 70.0, 6.0));

        assertTrue(direction.isPresent());
        assertEquals(0.6, direction.get().x, 1.0E-9);
        assertEquals(0.0, direction.get().y, 1.0E-9);
        assertEquals(-0.8, direction.get().z, 1.0E-9);
    }
}
