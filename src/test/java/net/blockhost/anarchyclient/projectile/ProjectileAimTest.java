package net.blockhost.anarchyclient.projectile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.blockhost.anarchyclient.rotation.Rotation;
import org.junit.jupiter.api.Test;

class ProjectileAimTest {

    @Test
    void exposesKnownProjectilePhysicsDefaults() {
        assertEquals(3.0, ProjectileAim.throwableVelocity("bow"));
        assertEquals(1.5, ProjectileAim.throwableVelocity("snowball"));
        assertEquals(0.05, ProjectileAim.gravity("trident"));
        assertEquals(0.03, ProjectileAim.gravity("egg"));
        assertEquals(0.5, ProjectileAim.throwableVelocity("potion"));
        assertEquals(0.05, ProjectileAim.gravity("potion"));
        assertEquals(0.07, ProjectileAim.gravity("experience_bottle"));
        // A potion leaves 20 degrees above the view, so a level launch needs the view 20 degrees down.
        assertEquals(
                20.0F,
                ProjectileAim.viewForLaunch(new Rotation(0.0F, 0.0F), "potion").pitch());
        assertEquals(
                90.0F,
                ProjectileAim.viewForLaunch(new Rotation(0.0F, 80.0F), "potion").pitch());
    }
}
