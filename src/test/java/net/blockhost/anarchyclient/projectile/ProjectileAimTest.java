package net.blockhost.anarchyclient.projectile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectileAimTest {

    @Test
    void exposesKnownProjectilePhysicsDefaults() {
        assertEquals(3.0, ProjectileAim.throwableVelocity("bow"));
        assertEquals(1.5, ProjectileAim.throwableVelocity("snowball"));
        assertEquals(0.05, ProjectileAim.gravity("trident"));
        assertEquals(0.03, ProjectileAim.gravity("egg"));
    }
}
